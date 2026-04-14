-- DIRECT 타입 채팅방 중 같은 두 유저 간 중복된 방 병합
-- 이유: findByTwoUsers 쿼리가 유니크 결과를 기대하지만 중복 방으로 인해 2개 이상 반환됨
-- 해결: 메시지를 하나의 방으로 합치고 중복 방 제거
--
SET @OLD_SQL_MODE = @@SESSION.sql_mode;
SET SESSION sql_mode = TRIM(BOTH ',' FROM REPLACE(REPLACE(REPLACE(REPLACE(CONCAT(',', @@SESSION.sql_mode, ','), ',NO_ZERO_DATE,', ','), ',NO_ZERO_IN_DATE,', ','), ',STRICT_TRANS_TABLES,', ','), ',STRICT_ALL_TABLES,', ','));

-- [엣지케이스 처리]
-- 1. 재시도 시 매핑 테이블 보호: NOT EXISTS로 기존 매핑 유지
-- 2. 연산자 우선순위: WHERE 조건에 괄호로 묶어 AND/OR 우선순위 명확화
-- 3. chat_room_member 병합: visible_message_from, left_at, last_read_at, custom_room_name, is_owner 모두 처리
--    - 기존 멤버 UPDATE 후 GROUP BY로 집계하여 orphan INSERT (다중 loser 시 PK 충돌 방지)
-- 4. notification_mute_setting 충돌: 여러 loser 방의 설정을 GROUP BY로 집계 후 INSERT
--    - 동일 사용자가 여러 loser 방에 뮤트 설정 시 MAX(is_muted)로 병합
-- 5. 메시지 중복 방지: from_room_id는 PK라 자연스럽게 중복 없음
-- 6. 롤백 지원: 매핑 테이블 DROP/CREATE 대신 재사용 패턴

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
-- 주의: AND/OR 우선순위 때문에 조건을 명확히 괄호로 묶음
-- LEFT JOIN 사용: 0명 방도 EXISTS 조건으로 포함되도록 보존
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
LEFT JOIN chat_room_member c1
  ON c1.chat_room_id = cr.id
LEFT JOIN chat_room_member c2
  ON c2.chat_room_id = cr.id
 AND c1.user_id < c2.user_id
WHERE cr.room_type = 'DIRECT'
  AND (
      (
          SELECT COUNT(*)
          FROM chat_room_member m
          WHERE m.chat_room_id = cr.id
      ) = 2
       OR EXISTS (
          SELECT 1 FROM temp_duplicate_room_map existing
          WHERE existing.from_room_id = cr.id OR existing.keep_room_id = cr.id
      )
  );

-- 2) 중복 방 중 어떤 방을 남기고 어떤 방을 지울지 매핑 테이블 생성
-- 매핑 테이블이 비어있는 경우에만 채움 (재시도 시 기존 매핑 유지)
-- ON DUPLICATE KEY UPDATE 제거: 재시도 시 매핑 방향이 뒤바뀌지 않도록 보호
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
  AND NOT EXISTS (SELECT 1 FROM temp_duplicate_room_map);

-- 3) 삭제 대상 방의 메시지를 keep 방으로 이동
-- 메시지는 PK(id)로 관리되므로 중복 키 충돌 없음
UPDATE chat_message cm
JOIN temp_duplicate_room_map m
  ON cm.chat_room_id = m.from_room_id
SET cm.chat_room_id = m.keep_room_id,
    cm.updated_at = cm.updated_at;

-- 4) 삭제 대상 방의 멤버십 상태를 keep 방으로 병합
-- visible_message_from: 더 이른 값(더 많은 메시지 조회 가능) 선택
-- left_at: 둘 중 하나라도 나간 경우 나간 것으로 처리 (더 이른 값 선택)
-- last_read_at: 더 나중에 읽은 값(최신 읽음 시점) 선택
-- custom_room_name: 사용자가 설정한 방 이름이 있으면 보존
-- is_owner: 하나라도 owner면 owner 유지 (OR 조건)
-- 여러 loser 방이 같은 keep 방으로 매핑될 수 있으므로 먼저 집계하여 중복 업데이트 방지

-- 4a) 기존 keep_room 멤버 업데이트
-- LEAST/GREATEST는 인자 중 NULL이 있으면 결과도 NULL이 되고, zero date('0000-00-00')도 문제 발생
UPDATE chat_room_member t
JOIN (
    SELECT
        m.keep_room_id,
        crm.user_id,
        NULLIF(MIN(crm.visible_message_from), '0000-00-00 00:00:00') AS min_visible_from,
        NULLIF(MIN(crm.left_at), '0000-00-00 00:00:00') AS min_left_at,
        NULLIF(MAX(crm.last_read_at), '0000-00-00 00:00:00') AS max_last_read_at,
        MAX(crm.custom_room_name) AS max_custom_room_name,
        MAX(CASE WHEN crm.is_owner THEN 1 ELSE 0 END) AS max_is_owner
    FROM temp_duplicate_room_map m
    JOIN chat_room_member crm ON crm.chat_room_id = m.from_room_id
    GROUP BY m.keep_room_id, crm.user_id
) la ON t.chat_room_id = la.keep_room_id AND t.user_id = la.user_id
SET t.visible_message_from = CASE
        WHEN t.visible_message_from IS NULL THEN la.min_visible_from
        WHEN la.min_visible_from IS NULL THEN t.visible_message_from
        ELSE LEAST(t.visible_message_from, la.min_visible_from)
    END,
    t.left_at = CASE
        WHEN t.left_at IS NULL THEN la.min_left_at
        WHEN la.min_left_at IS NULL THEN t.left_at
        ELSE LEAST(t.left_at, la.min_left_at)
    END,
    t.last_read_at = CASE
        WHEN t.last_read_at IS NULL THEN la.max_last_read_at
        WHEN la.max_last_read_at IS NULL THEN t.last_read_at
        ELSE GREATEST(t.last_read_at, la.max_last_read_at)
    END,
    t.custom_room_name = COALESCE(t.custom_room_name, la.max_custom_room_name),
    t.is_owner = (t.is_owner OR la.max_is_owner > 0),
    t.updated_at = t.updated_at;

-- 4b) keep_room에 없는 loser 멤버 INSERT (orphan member 처리)
-- 여러 loser 방이 같은 keep 방으로 매핑될 수 있으므로 GROUP BY로 집계하여 PK 충돌 방지
INSERT INTO chat_room_member (
    chat_room_id, user_id, last_read_at, created_at, updated_at,
    visible_message_from, left_at, custom_room_name, is_owner
)
SELECT
    m.keep_room_id,
    crm.user_id,
    NULLIF(MAX(crm.last_read_at), '0000-00-00 00:00:00') AS last_read_at,
    MIN(crm.created_at) AS created_at,
    MAX(crm.updated_at) AS updated_at,
    NULLIF(MIN(crm.visible_message_from), '0000-00-00 00:00:00') AS visible_message_from,
    NULLIF(MIN(crm.left_at), '0000-00-00 00:00:00') AS left_at,
    MAX(crm.custom_room_name) AS custom_room_name,
    MAX(CASE WHEN crm.is_owner THEN 1 ELSE 0 END) AS is_owner
FROM temp_duplicate_room_map m
JOIN chat_room_member crm ON crm.chat_room_id = m.from_room_id
LEFT JOIN chat_room_member existing
    ON existing.chat_room_id = m.keep_room_id
    AND existing.user_id = crm.user_id
WHERE existing.chat_room_id IS NULL
GROUP BY m.keep_room_id, crm.user_id;

-- 5) 삭제 대상 방의 알림 뮤트 설정을 keep 방으로 이동
-- 여러 loser 방이 같은 keep 방으로 매핑될 수 있으므로 먼저 집계하여 UNIQUE 충돌 방지

-- 5a) loser 방들의 뮤트 설정을 keep_room_id 기준으로 집계
-- 동일 사용자가 여러 loser 방에 뮤트 설정을 가진 경우 MAX(is_muted)로 병합
CREATE TEMPORARY TABLE temp_mute_setting_agg AS
SELECT
    m.keep_room_id,
    nms.user_id,
    MAX(nms.is_muted) AS is_muted
FROM temp_duplicate_room_map m
JOIN notification_mute_setting nms
    ON nms.target_id = m.from_room_id
    AND nms.target_type = 'CHAT_ROOM'
GROUP BY m.keep_room_id, nms.user_id;

-- 5b) 집계된 설정과 충돌하는 기존 keep 방 뮤트 설정 삭제
DELETE nms
FROM notification_mute_setting nms
JOIN temp_mute_setting_agg agg
    ON nms.target_id = agg.keep_room_id
    AND nms.user_id = agg.user_id
WHERE nms.target_type = 'CHAT_ROOM';

-- 5c) 집계된 뮤트 설정을 keep 방에 INSERT
INSERT INTO notification_mute_setting (user_id, target_type, target_id, is_muted, created_at, updated_at)
SELECT
    agg.user_id,
    'CHAT_ROOM',
    agg.keep_room_id,
    agg.is_muted,
    NOW(),
    NOW()
FROM temp_mute_setting_agg agg;

-- 5d) loser 방의 뮤트 설정 삭제 (이미 집계되어 이동 완료)
DELETE nms
FROM notification_mute_setting nms
JOIN temp_duplicate_room_map m
    ON nms.target_id = m.from_room_id
WHERE nms.target_type = 'CHAT_ROOM';

-- 임시 테이블 정리
DROP TEMPORARY TABLE IF EXISTS temp_mute_setting_agg;

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
-- MAX(created_at)으로 최신 메시지 선택, 동일 타임스탬프 시 id로 타임브레이커
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
        SELECT chat_room_id, MAX(created_at) AS max_created_at
        FROM chat_message
        GROUP BY chat_room_id
    ) cm2 ON cm2.chat_room_id = cm1.chat_room_id AND cm2.max_created_at = cm1.created_at
    WHERE cm1.id = (
        SELECT MAX(id)
        FROM chat_message
        WHERE chat_room_id = cm1.chat_room_id
          AND created_at = cm2.max_created_at
    )
) latest_msg ON latest_msg.chat_room_id = cr.id
SET cr.last_message_content = latest_msg.content,
    cr.last_message_sent_at = latest_msg.created_at;

-- 9) 임시 테이블 정리
DROP TABLE IF EXISTS temp_duplicate_room_map;
DROP TABLE IF EXISTS temp_direct_room_pairs;

SET SESSION sql_mode = @OLD_SQL_MODE;
