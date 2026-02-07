-- 암호화된 메시지는 Base64 인코딩으로 인해 원본보다 길어지므로 TEXT 타입으로 변경
ALTER TABLE chat_message
    MODIFY COLUMN content TEXT NOT NULL;

ALTER TABLE chat_room
    MODIFY COLUMN last_message_content TEXT;
