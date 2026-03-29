-- 성능 최적화 인덱스 추가
-- 목적: 자주 사용되는 조회 조건에 대한 인덱스 추가

-- chat_message 테이블: 채팅방별 시간순 조회 (findByChatRoomId ORDER BY created_at)
CREATE INDEX idx_chat_message_room_created_at ON chat_message (chat_room_id, created_at DESC);

-- notification_inbox 테이블: 읽지 않은 알림 카운트 (countByUserIdAndIsReadFalse)
CREATE INDEX idx_notification_inbox_user_read ON notification_inbox (user_id, is_read);
