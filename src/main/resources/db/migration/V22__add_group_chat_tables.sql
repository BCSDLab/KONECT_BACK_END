CREATE TABLE IF NOT EXISTS group_chat_room
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    club_id    INT                                                            NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_group_chat_room_club_id UNIQUE (club_id),
    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_chat_message
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    room_id    INT                                                            NOT NULL,
    sender_id  INT                                                            NOT NULL,
    content    VARCHAR(1000)                                                  NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (room_id) REFERENCES group_chat_room (id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification_mute_setting
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT                                                            NOT NULL,
    target_type   VARCHAR(50)                                                    NOT NULL,
    target_id     INT                                                            NULL,
    target_id_key INT GENERATED ALWAYS AS (IFNULL(target_id, -1)) STORED,
    is_muted      BOOLEAN                                                        NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_notification_mute_setting_user_target UNIQUE (user_id, target_type, target_id_key),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_chat_read_status
(
    room_id      INT       NOT NULL,
    user_id      INT       NOT NULL,
    last_read_at TIMESTAMP NOT NULL,

    PRIMARY KEY (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES group_chat_room (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_group_chat_message_room_created_at
    ON group_chat_message (room_id, created_at DESC);

CREATE INDEX idx_group_chat_message_room_id
    ON group_chat_message (room_id, id DESC);

CREATE INDEX idx_notification_mute_setting_target
    ON notification_mute_setting (target_type, target_id, is_muted);

INSERT INTO group_chat_room (club_id, created_at, updated_at)
SELECT id, NOW(), NOW() FROM club
WHERE id NOT IN (SELECT club_id FROM group_chat_room);

INSERT INTO group_chat_read_status (room_id, user_id, last_read_at)
SELECT r.id, cm.user_id, cm.created_at
FROM group_chat_room r
    JOIN club_member cm ON cm.club_id = r.club_id
    LEFT JOIN group_chat_read_status grs ON grs.room_id = r.id
    AND grs.user_id = cm.user_id
WHERE grs.room_id IS NULL;
