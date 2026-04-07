-- SYSTEM_ADMIN(1번)이 있는 DIRECT 채팅방에서 다른 어드민 멤버십 제거
-- 이유: 어드민이 멤버로 추가되면 findByTwoUsers에서 해당 방을 찾지 못해 중복 생성됨
-- 참고: https://github.com/BCSDLab/KONECT_BACK_END/issues/503

DELETE FROM chat_room_member
WHERE user_id IN (
    SELECT u.id
    FROM users u
    WHERE u.role = 'ADMIN'
    AND u.id != 1  -- SYSTEM_ADMIN(1번)은 제외
)
AND chat_room_id IN (
    SELECT DISTINCT crm.chat_room_id
    FROM chat_room_member crm
    JOIN chat_room cr ON crm.chat_room_id = cr.id
    WHERE crm.user_id = 1  -- SYSTEM_ADMIN(1번)이 있는 방
    AND cr.room_type = 'DIRECT'  -- DIRECT 타입 방만
);
