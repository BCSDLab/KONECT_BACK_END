-- DIRECT 타입 채팅방 중 같은 두 유저 간 중복된 방 병합
-- 이유: findByTwoUsers 쿼리가 유니크 결과를 기대하지만 중복 방으로 인해 2개 이상 반환됨
-- 해결: 메시지를 하나의 방으로 합치고 중복 방 제거

-- 임시 테이블 대신 실제 테이블 사용 (MySQL 임시 테이블 재참조 제한 회피)
DROP TABLE IF EXISTS temp_direct_room_pairs;
DROP TABLE IF EXISTS temp_duplicate_room_map;

-- 1) DIRECT 방 중 "정확히 2명"인 방만 유저쌍 단위로 펼침
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
  ) = 2;

-- 2) 중복 방 중 어떤 방을 남기고 어떤 방을 지울지 매핑 테이블 생성
CREATE TABLE temp_duplicate_room_map AS
WITH ranked_rooms AS (
    SELECT
        room_id,
        user1_id,
        user2_id,
        ROW_NUMBER() OVER (
            PARTITION BY user1_id, user2_id
            ORDER BY
                COALESCE(last_message_at, created_at) DESC,
                created_at DESC,
                room_id DESC
        ) AS rn,
        COUNT(*) OVER (
            PARTITION BY user1_id, user2_id
        ) AS room_count
    FROM temp_direct_room_pairs
)
SELECT
    loser.room_id AS from_room_id,
    winner.room_id AS keep_room_id,
    loser.user1_id,
    loser.user2_id
FROM ranked_rooms loser
JOIN ranked_rooms winner
  ON winner.user1_id = loser.user1_id
 AND winner.user2_id = loser.user2_id
 AND winner.rn = 1
WHERE loser.room_count > 1
  AND loser.rn > 1;

-- 3) 삭제 대상 방의 메시지를 keep 방으로 이동
UPDATE chat_message cm
JOIN temp_duplicate_room_map m
  ON cm.chat_room_id = m.from_room_id
SET cm.chat_room_id = m.keep_room_id;

-- 4) 삭제 대상 방의 멤버십 삭제
DELETE crm
FROM chat_room_member crm
JOIN temp_duplicate_room_map m
  ON crm.chat_room_id = m.from_room_id;

-- 5) 삭제 대상 방 삭제
DELETE cr
FROM chat_room cr
JOIN temp_duplicate_room_map m
  ON cr.id = m.from_room_id;

-- 6) 임시 테이블 정리
DROP TABLE IF EXISTS temp_duplicate_room_map;
DROP TABLE IF EXISTS temp_direct_room_pairs;
