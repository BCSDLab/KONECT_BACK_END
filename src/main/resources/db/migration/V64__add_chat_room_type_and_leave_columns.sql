ALTER TABLE chat_room
    ADD COLUMN room_type VARCHAR(20) NULL AFTER last_message_sent_at;

UPDATE chat_room
SET room_type = CASE
    WHEN club_id IS NULL THEN 'DIRECT'
    ELSE 'GROUP'
END;

ALTER TABLE chat_room
    MODIFY COLUMN room_type VARCHAR(20) NOT NULL;

ALTER TABLE chat_room_member
    ADD COLUMN visible_message_from TIMESTAMP NULL AFTER last_read_at,
    ADD COLUMN left_at TIMESTAMP NULL AFTER visible_message_from;

CREATE INDEX idx_chat_room_member_user_left_at
    ON chat_room_member (user_id, left_at);

CREATE INDEX idx_chat_room_room_type
    ON chat_room (room_type);
