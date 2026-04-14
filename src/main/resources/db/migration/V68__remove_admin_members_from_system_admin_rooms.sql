-- SYSTEM_ADMIN(1번)이 있는 DIRECT 채팅방에서 다른 어드민 멤버십 제거
-- 이유: 어드민이 멤버로 추가되면 findByTwoUsers에서 해당 방을 찾지 못해 중복 생성됨
-- 참고: https://github.com/BCSDLab/KONECT_BACK_END/issues/503

DELETE crm
FROM chat_room_member AS crm
JOIN users AS u
  ON u.id = crm.user_id
JOIN chat_room AS cr
  ON cr.id = crm.chat_room_id
JOIN chat_room_member AS system_member
  ON system_member.chat_room_id = crm.chat_room_id
WHERE u.role = 'ADMIN'
  AND u.id <> 1
  AND cr.room_type = 'DIRECT'
  AND system_member.user_id = 1;
