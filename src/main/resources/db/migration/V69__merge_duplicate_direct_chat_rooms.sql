-- DIRECT 타입 채팅방 중 같은 두 유저 간 중복된 방 병합
-- 이유: findByTwoUsers 쿼리가 유니크 결과를 기대하지만 중복 방으로 인해 2개 이상 반환됨
-- 해결: 메시지를 하나의 방으로 합치고 중복 방 제거

-- Step 1: 중복 방 정보를 임시 테이블에 저장 (메인 방: 가장 최근 메시지가 있는 방, 없으면 최신 생성)
CREATE TEMPORARY TABLE IF NOT EXISTS temp_duplicate_rooms AS
SELECT
    crm1.user_id AS user1_id,
    crm2.user_id AS user2_id,
    -- 메인 방 선정: 가장 최근 메시지 시간, 없으면 생성 시간
    (
        SELECT cr_sub.id
        FROM chat_room cr_sub
        JOIN chat_room_member crm_sub1 ON crm_sub1.chat_room_id = cr_sub.id AND crm_sub1.user_id = crm1.user_id
        JOIN chat_room_member crm_sub2 ON crm_sub2.chat_room_id = cr_sub.id AND crm_sub2.user_id = crm2.user_id
        WHERE cr_sub.room_type = 'DIRECT'
        ORDER BY (
            SELECT MAX(cm.created_at)
            FROM chat_message cm
            WHERE cm.chat_room_id = cr_sub.id
        ) DESC, cr_sub.created_at DESC
        LIMIT 1
    ) AS keep_room_id,
    -- 삭제할 방들
    GROUP_CONCAT(
        DISTINCT cr_del.id
        ORDER BY cr_del.id
        SEPARATOR ','
    ) AS delete_room_ids
FROM chat_room cr_del
JOIN chat_room_member crm1 ON crm1.chat_room_id = cr_del.id
JOIN chat_room_member crm2 ON crm2.chat_room_id = cr_del.id AND crm1.user_id < crm2.user_id
WHERE cr_del.room_type = 'DIRECT'
GROUP BY crm1.user_id, crm2.user_id
HAVING COUNT(DISTINCT cr_del.id) > 1;

-- Step 2: 삭제할 방의 메시지를 메인 방으로 이동
-- 임시 테이블에서 삭제 대상 방 ID 파싱하여 업데이트
UPDATE chat_message cm
JOIN (
    SELECT
        SUBSTRING_INDEX(SUBSTRING_INDEX(t.delete_room_ids, ',', numbers.n), ',', -1) AS from_room_id,
        t.keep_room_id AS to_room_id
    FROM temp_duplicate_rooms t
    CROSS JOIN (
        SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    ) numbers
    WHERE t.delete_room_ids IS NOT NULL
      AND CHAR_LENGTH(t.delete_room_ids) - CHAR_LENGTH(REPLACE(t.delete_room_ids, ',', '')) >= numbers.n - 1
) move_map ON cm.chat_room_id = move_map.from_room_id
SET cm.chat_room_id = move_map.to_room_id
WHERE move_map.from_room_id != move_map.to_room_id;

-- Step 3: 중복 방의 멤버십 삭제
DELETE crm
FROM chat_room_member crm
JOIN (
    SELECT DISTINCT
        SUBSTRING_INDEX(SUBSTRING_INDEX(t.delete_room_ids, ',', numbers.n), ',', -1) AS delete_room_id
    FROM temp_duplicate_rooms t
    CROSS JOIN (
        SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    ) numbers
    WHERE t.delete_room_ids IS NOT NULL
      AND CHAR_LENGTH(t.delete_room_ids) - CHAR_LENGTH(REPLACE(t.delete_room_ids, ',', '')) >= numbers.n - 1
) to_delete ON crm.chat_room_id = to_delete.delete_room_id;

-- Step 4: 중복 방 삭제
DELETE cr
FROM chat_room cr
JOIN (
    SELECT DISTINCT
        SUBSTRING_INDEX(SUBSTRING_INDEX(t.delete_room_ids, ',', numbers.n), ',', -1) AS delete_room_id
    FROM temp_duplicate_rooms t
    CROSS JOIN (
        SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    ) numbers
    WHERE t.delete_room_ids IS NOT NULL
      AND CHAR_LENGTH(t.delete_room_ids) - CHAR_LENGTH(REPLACE(t.delete_room_ids, ',', '')) >= numbers.n - 1
) to_delete ON cr.id = to_delete.delete_room_id
WHERE cr.room_type = 'DIRECT';

-- Step 5: 임시 테이블 정리
DROP TEMPORARY TABLE IF EXISTS temp_duplicate_rooms;
