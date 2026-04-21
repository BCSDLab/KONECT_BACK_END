-- 채팅방 목록/관리 쿼리는 chat_room.last_message_*를 직접 사용하므로
-- 기존 메시지 이력이 있는 방도 최신 메시지 메타데이터를 다시 맞춰 준다.
-- MAX(created_at)으로 최신 메시지를 고르고, 같은 시각이면 id로 타이브레이크한다.
UPDATE chat_room cr
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
