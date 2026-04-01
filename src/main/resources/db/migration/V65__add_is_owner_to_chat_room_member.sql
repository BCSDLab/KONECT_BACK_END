ALTER TABLE chat_room_member
    ADD COLUMN is_owner BOOLEAN NOT NULL DEFAULT FALSE AFTER custom_room_name;
