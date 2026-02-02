UPDATE chat_message
SET content = SUBSTRING(content, 1, 1000)
WHERE CHAR_LENGTH(content) > 1000;

ALTER TABLE chat_message
    MODIFY COLUMN content VARCHAR(1000) NOT NULL;
