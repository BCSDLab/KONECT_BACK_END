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

CREATE TABLE IF NOT EXISTS message_read_status
(
    message_id INT NOT NULL,
    user_id    INT NOT NULL,
    read_at    TIMESTAMP NOT NULL,

    PRIMARY KEY (message_id, user_id),
    FOREIGN KEY (message_id) REFERENCES group_chat_message (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_chat_notification_setting
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    room_id    INT                                                            NOT NULL,
    user_id    INT                                                            NOT NULL,
    is_muted   BOOLEAN                                                        NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_group_chat_notification_setting_room_id_user_id UNIQUE (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES group_chat_room (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_group_chat_message_room_created ON group_chat_message (room_id, created_at DESC);
CREATE INDEX idx_message_read_status_message ON message_read_status (message_id);

INSERT INTO group_chat_room (club_id, created_at, updated_at)
SELECT id, NOW(), NOW() FROM club
WHERE id NOT IN (SELECT club_id FROM group_chat_room);
