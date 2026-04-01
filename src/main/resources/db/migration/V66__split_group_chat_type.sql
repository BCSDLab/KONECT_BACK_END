-- 동아리가 연결된 기존 GROUP 채팅방을 CLUB_GROUP으로 마이그레이션
UPDATE chat_room
SET room_type = 'CLUB_GROUP'
WHERE room_type = 'GROUP'
  AND club_id IS NOT NULL;
