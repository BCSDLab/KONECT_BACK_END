-- DIRECT 타입 채팅방 중 같은 두 유저 간 중복된 방 병합
-- 이유: findByTwoUsers 쿼리가 유니크 결과를 기대하지만 중복 방으로 인해 2개 이상 반환됨
-- 해결: 메시지를 하나의 방으로 합치고 중복 방 제거

-- 임시 테이블 대신 실제 테이블 사용 (MySQL 임시 테이블 재참조 제한 회피)
-- 재시도 가능하도록: 기존 매핑 테이블이 있으면 스킵, 없으면 생성
DROP TABLE IF EXISTS temp_direct_room_pairs;

-- 이전 실행에서 남은 매핑 테이블이 있으면 재사용 (재시도 시)
-- 없으면 새로 생성
CREATE TABLE IF NOT EXISTS temp_duplicate_room_map (
    from_room_id INT PRIMARY KEY,
    keep_room_id INT NOT NULL,
    user1_id INT NOT NULL,
    user2_id INT NOT NULL
);

-- 1) DIRECT 방 중 "정확히 2명"인 방만 유저쌍 단위로 펼침
-- 또는 이미 매핑 테이블에 있는 방도 포함 (재시도 시 0명이 된 방 처리)
CREATE TABLE temp_direct_room_pairs AS
SELECT
    cr.id AS room_id,
    LEAST(c1.user_id, c2.user_id) AS user1_id,
    GREATEST(c1.user_id, c2.user_id) AS user2_id,
    cr.created_at,
    (
        SELECT MAX(cm.created_at)
        FROM chat_message cm
        WHERE cm.chat_room_id = cr.id
    ) AS last_message_at
FROM chat_room cr
JOIN chat_room_member c1
  ON c1.chat_room_id = cr.id
JOIN chat_room_member c2
  ON c2.chat_room_id = cr.id
 AND c1.user_id < c2.user_id
WHERE cr.room_type = 'DIRECT'
  AND (
      SELECT COUNT(*)
      FROM chat_room_member m
      WHERE m.chat_room_id = cr.id
  ) = 2
   OR EXISTS (
      SELECT 1 FROM temp_duplicate_room_map existing
      WHERE existing.from_room_id = cr.id OR existing.keep_room_id = cr.id
  );

-- 2) 중복 방 중 어떤 방을 남기고 어떤 방을 지울지 매핑 테이블 생성
-- 매핑 테이블이 비어있는 경우에만 채움 (재시도 시 기존 매핑 유지)
INSERT INTO temp_duplicate_room_map (from_room_id, keep_room_id, user1_id, user2_id)
SELECT
    loser.room_id AS from_room_id,
    winner.room_id AS keep_room_id,
    loser.user1_id,
    loser.user2_id
FROM (
    SELECT
        room_id,
        user1_id,
        user2_id,
        ROW_NUMBER() OVER (
            PARTITION BY user1_id, user2_id
            ORDER BY
                (last_message_at IS NOT NULL) DESC,
                last_message_at DESC,
                created_at DESC,
                room_id DESC
        ) AS rn,
        COUNT(*) OVER (
            PARTITION BY user1_id, user2_id
        ) AS room_count
    FROM temp_direct_room_pairs
) loser
JOIN (
    SELECT
        room_id,
        user1_id,
        user2_id,
        ROW_NUMBER() OVER (
            PARTITION BY user1_id, user2_id
            ORDER BY
                (last_message_at IS NOT NULL) DESC,
                last_message_at DESC,
                created_at DESC,
                room_id DESC
        ) AS rn
    FROM temp_direct_room_pairs
) winner
  ON winner.user1_id = loser.user1_id
 AND winner.user2_id = loser.user2_id
 AND winner.rn = 1
WHERE loser.room_count > 1
  AND loser.rn > 1
ON DUPLICATE KEY UPDATE
    keep_room_id = VALUES(keep_room_id),
    user1_id = VALUES(user1_id),
    user2_id = VALUES(user2_id);

-- 3) 삭제 대상 방의 메시지를 keep 방으로 이동
UPDATE chat_message cm
JOIN temp_duplicate_room_map m
  ON cm.chat_room_id = m.from_room_id
SET cm.chat_room_id = m.keep_room_id,
    cm.updated_at = cm.updated_at;

-- 4) 삭제 대상 방의 멤버십 상태를 keep 방으로 병합 (visible_message_from, left_at, last_read_at, custom_room_name 보존)
-- visible_message_from: 더 이른 값(더 많은 메시지 조회 가능) 선택
-- left_at: 둘 중 하나라도 나간 경우 나간 것으로 처리 (더 이른 값 선택)
-- last_read_at: 더 나중에 읽은 값(최신 읽음 시점) 선택
-- custom_room_name: 사용자가 설정한 방 이름이 있으면 보존
-- 여러 loser 방이 같은 keep 방으로 매핑될 수 있으므로 먼저 집계하여 중복 업데이트 방지
UPDATE chat_room_member t
JOIN (
    SELECT
        m.keep_room_id,
        crm.user_id,
        MIN(crm.visible_message_from) AS min_visible_from,
        MIN(crm.left_at) AS min_left_at,
        MAX(crm.last_read_at) AS max_last_read_at,
        MAX(crm.custom_room_name) AS max_custom_room_name
    FROM temp_duplicate_room_map m
    JOIN chat_room_member crm ON crm.chat_room_id = m.from_room_id
    GROUP BY m.keep_room_id, crm.user_id
) la ON t.chat_room_id = la.keep_room_id AND t.user_id = la.user_id
SET t.visible_message_from = LEAST(t.visible_message_from, la.min_visible_from),
    t.left_at = CASE
        WHEN t.left_at IS NULL THEN la.min_left_at
        WHEN la.min_left_at IS NULL THEN t.left_at
        ELSE LEAST(t.left_at, la.min_left_at)
    END,
    t.last_read_at = GREATEST(t.last_read_at, la.max_last_read_at),
    t.custom_room_name = COALESCE(t.custom_room_name, la.max_custom_room_name),
    t.updated_at = t.updated_at;

-- 5) 삭제 대상 방의 알림 뮤트 설정을 keep 방으로 이동
-- 이미 keep 방에 뮤트 설정이 있으면 from_room 설정은 삭제됨 (UNIQUE 제약조건)
UPDATE notification_mute_setting nms
JOIN temp_duplicate_room_map m
  ON nms.target_id = m.from_room_id
SET nms.target_id = m.keep_room_id,
    nms.updated_at = nms.updated_at
WHERE nms.target_type = 'CHAT_ROOM';

-- 6) 삭제 대상 방의 멤버십 삭제
DELETE crm
FROM chat_room_member crm
JOIN temp_duplicate_room_map m
  ON crm.chat_room_id = m.from_room_id;

-- 7) 삭제 대상 방 삭제
DELETE cr
FROM chat_room cr
JOIN temp_duplicate_room_map m
  ON cr.id = m.from_room_id;

-- 8) 남은 방의 last_message_content와 last_message_sent_at 갱신
-- 타임스탬프 동일 시 id가 큰 메시지(나중에 생성된)를 선택하여 결정론적으로 만듦
UPDATE chat_room cr
JOIN temp_duplicate_room_map m
  ON cr.id = m.keep_room_id
LEFT JOIN (
    SELECT
        cm1.chat_room_id,
        cm1.content,
        cm1.created_at
    FROM chat_message cm1
    JOIN (
        SELECT chat_room_id, MAX(id) AS max_id
        FROM chat_message
        GROUP BY chat_room_id
    ) cm2 ON cm2.chat_room_id = cm1.chat_room_id AND cm2.max_id = cm1.id
) latest_msg ON latest_msg.chat_room_id = cr.id
SET cr.last_message_content = latest_msg.content,
    cr.last_message_sent_at = latest_msg.created_at;

-- 9) 임시 테이블 정리
DROP TABLE IF EXISTS temp_duplicate_room_map;
DROP TABLE IF EXISTS temp_direct_room_pairs;
