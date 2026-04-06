-- 누락된 chat_room_member 데이터를 복구합니다.
-- 과거 버그로 인해 club_member는 있지만 chat_room_member가 없는 경우를 복구합니다.
-- last_read_at은 동아리 가입 시점(club_member.created_at)으로 설정합니다.

INSERT INTO chat_room_member (
    chat_room_id,
    user_id,
    last_read_at,
    created_at,
    updated_at
)
SELECT
    cr.id AS chat_room_id,
    cm.user_id AS user_id,
    cm.created_at AS last_read_at,
    NOW() AS created_at,
    NOW() AS updated_at
FROM club_member cm
JOIN chat_room cr ON cr.club_id = cm.club_id
LEFT JOIN chat_room_member crm
    ON crm.chat_room_id = cr.id
    AND crm.user_id = cm.user_id
WHERE crm.chat_room_id IS NULL;
