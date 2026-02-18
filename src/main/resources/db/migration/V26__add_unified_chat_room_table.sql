ALTER TABLE chat_room
    ADD COLUMN club_id INT NULL;

ALTER TABLE chat_room
    ADD CONSTRAINT fk_chat_room_club_id FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uq_chat_room_group_club_id
    ON chat_room (club_id);

SET @receiver_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'chat_message'
      AND kcu.COLUMN_NAME = 'receiver_id'
      AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_receiver_fk_sql = IF(
    @receiver_fk_name IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE chat_message DROP FOREIGN KEY `', @receiver_fk_name, '`')
);

PREPARE drop_receiver_fk_stmt FROM @drop_receiver_fk_sql;
EXECUTE drop_receiver_fk_stmt;
DEALLOCATE PREPARE drop_receiver_fk_stmt;

ALTER TABLE chat_message
    DROP COLUMN receiver_id;

ALTER TABLE chat_message
    DROP COLUMN is_read;

CREATE TABLE IF NOT EXISTS chat_room_member
(
    chat_room_id  INT                                                            NOT NULL,
    user_id       INT                                                            NOT NULL,
    last_read_at  TIMESTAMP                                                      NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (chat_room_id, user_id),
    FOREIGN KEY (chat_room_id) REFERENCES chat_room (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_room_member_user_id
    ON chat_room_member (user_id, chat_room_id);

CREATE INDEX idx_chat_room_member_room_last_read
    ON chat_room_member (chat_room_id, last_read_at);

INSERT INTO chat_room_member (chat_room_id, user_id, last_read_at, created_at, updated_at)
SELECT src.chat_room_id,
       src.user_id,
       src.last_read_at,
       src.created_at,
       src.updated_at
FROM (
         SELECT cr.id AS chat_room_id,
                cr.sender_id AS user_id,
                COALESCE(cr.last_message_sent_at, cr.created_at) AS last_read_at,
                cr.created_at AS created_at,
                NOW() AS updated_at
         FROM chat_room cr
         WHERE cr.sender_id IS NOT NULL
     ) src
ON DUPLICATE KEY UPDATE
    last_read_at = GREATEST(chat_room_member.last_read_at, src.last_read_at);

INSERT INTO chat_room_member (chat_room_id, user_id, last_read_at, created_at, updated_at)
SELECT src.chat_room_id,
       src.user_id,
       src.last_read_at,
       src.created_at,
       src.updated_at
FROM (
         SELECT cr.id AS chat_room_id,
                cr.receiver_id AS user_id,
                COALESCE(cr.last_message_sent_at, cr.created_at) AS last_read_at,
                cr.created_at AS created_at,
                NOW() AS updated_at
         FROM chat_room cr
         WHERE cr.receiver_id IS NOT NULL
     ) src
ON DUPLICATE KEY UPDATE
    last_read_at = GREATEST(chat_room_member.last_read_at, src.last_read_at);

SET @chat_room_sender_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'chat_room'
      AND kcu.COLUMN_NAME = 'sender_id'
      AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_chat_room_sender_fk_sql = IF(
    @chat_room_sender_fk_name IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE chat_room DROP FOREIGN KEY `', @chat_room_sender_fk_name, '`')
);

PREPARE drop_chat_room_sender_fk_stmt FROM @drop_chat_room_sender_fk_sql;
EXECUTE drop_chat_room_sender_fk_stmt;
DEALLOCATE PREPARE drop_chat_room_sender_fk_stmt;

SET @chat_room_receiver_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'chat_room'
      AND kcu.COLUMN_NAME = 'receiver_id'
      AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_chat_room_receiver_fk_sql = IF(
    @chat_room_receiver_fk_name IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE chat_room DROP FOREIGN KEY `', @chat_room_receiver_fk_name, '`')
);

PREPARE drop_chat_room_receiver_fk_stmt FROM @drop_chat_room_receiver_fk_sql;
EXECUTE drop_chat_room_receiver_fk_stmt;
DEALLOCATE PREPARE drop_chat_room_receiver_fk_stmt;

ALTER TABLE chat_room
    DROP COLUMN sender_id,
    DROP COLUMN receiver_id;

INSERT INTO chat_room (
    last_message_content,
    last_message_sent_at,
    club_id,
    created_at,
    updated_at
)
SELECT gm.content,
       gm.created_at,
       gcr.club_id,
       gcr.created_at,
       gcr.updated_at
FROM group_chat_room gcr
         LEFT JOIN (
    SELECT m.room_id,
           m.content,
           m.created_at
    FROM group_chat_message m
             JOIN (
        SELECT room_id, MAX(id) AS max_id
        FROM group_chat_message
        GROUP BY room_id
    ) lm ON lm.max_id = m.id
) gm ON gm.room_id = gcr.id
         LEFT JOIN chat_room cr ON cr.club_id = gcr.club_id
WHERE cr.id IS NULL;

INSERT INTO chat_room_member (chat_room_id, user_id, last_read_at, created_at, updated_at)
SELECT src.chat_room_id,
       src.user_id,
       src.last_read_at,
       src.created_at,
       src.updated_at
FROM (
         SELECT cr.id AS chat_room_id,
                cm.user_id AS user_id,
                COALESCE(grs.last_read_at, cm.created_at) AS last_read_at,
                cm.created_at AS created_at,
                NOW() AS updated_at
         FROM chat_room cr
                  JOIN club_member cm ON cm.club_id = cr.club_id
                  LEFT JOIN group_chat_room gcr ON gcr.club_id = cr.club_id
                  LEFT JOIN group_chat_read_status grs ON grs.room_id = gcr.id
             AND grs.user_id = cm.user_id
         WHERE cr.club_id IS NOT NULL
     ) src
ON DUPLICATE KEY UPDATE
    last_read_at = GREATEST(chat_room_member.last_read_at, src.last_read_at);

INSERT INTO chat_message (
    chat_room_id,
    sender_id,
    content,
    created_at,
    updated_at
)
SELECT cr.id,
       gm.sender_id,
       gm.content,
       gm.created_at,
       gm.updated_at
FROM group_chat_message gm
         JOIN group_chat_room gcr ON gcr.id = gm.room_id
         JOIN chat_room cr ON cr.club_id = gcr.club_id
         LEFT JOIN chat_message cm
                   ON cm.chat_room_id = cr.id
                       AND cm.sender_id = gm.sender_id
                       AND cm.content = gm.content
                       AND cm.created_at = gm.created_at
WHERE cm.id IS NULL;

UPDATE chat_room cr
    JOIN (
    SELECT cm.chat_room_id,
           cm.content,
           cm.created_at
    FROM chat_message cm
             JOIN (
        SELECT chat_room_id, MAX(id) AS max_id
        FROM chat_message
        GROUP BY chat_room_id
    ) lm ON lm.max_id = cm.id
) lm ON lm.chat_room_id = cr.id
SET cr.last_message_content = lm.content,
    cr.last_message_sent_at = lm.created_at,
    cr.updated_at = GREATEST(cr.updated_at, lm.created_at)
WHERE cr.club_id IS NOT NULL;

INSERT INTO notification_mute_setting (user_id, target_type, target_id, is_muted, created_at, updated_at)
SELECT nms.user_id,
       'CHAT_ROOM',
       nms.target_id,
       nms.is_muted,
       nms.created_at,
       nms.updated_at
FROM notification_mute_setting nms
         LEFT JOIN notification_mute_setting existed
                   ON existed.user_id = nms.user_id
                       AND existed.target_type = 'CHAT_ROOM'
                       AND existed.target_id = nms.target_id
WHERE nms.target_type = 'DIRECT_CHAT_ROOM'
  AND existed.id IS NULL;

INSERT INTO notification_mute_setting (user_id, target_type, target_id, is_muted, created_at, updated_at)
SELECT nms.user_id,
       'CHAT_ROOM',
       cr.id,
       nms.is_muted,
       nms.created_at,
       nms.updated_at
FROM notification_mute_setting nms
         JOIN group_chat_room gcr ON gcr.id = nms.target_id
         JOIN chat_room cr ON cr.club_id = gcr.club_id
         LEFT JOIN notification_mute_setting existed
                   ON existed.user_id = nms.user_id
                       AND existed.target_type = 'CHAT_ROOM'
                       AND existed.target_id = cr.id
WHERE nms.target_type = 'GROUP_CHAT_ROOM'
  AND existed.id IS NULL;
