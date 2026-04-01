-- 관리자 1:1 채팅방 조회 쿼리 최적화를 위한 인덱스 추가
-- findAdminChatRoomsOptimized() 메소드 성능 개선

-- 1. 관리자 응답 여부 확인 및 unread count 계산용
CREATE INDEX idx_chat_message_room_sender
    ON chat_message (chat_room_id, sender_id);

-- 2. last_message_sent_at 정렬 최적화
CREATE INDEX idx_chat_room_last_message
    ON chat_room (club_id, last_message_sent_at DESC);
