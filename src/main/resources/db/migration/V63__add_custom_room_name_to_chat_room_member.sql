ALTER TABLE chat_room_member
    ADD COLUMN custom_room_name VARCHAR(30) NULL AFTER last_read_at;
