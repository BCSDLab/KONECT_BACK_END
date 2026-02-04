CREATE TABLE IF NOT EXISTS notification_device_token
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT                                                            NOT NULL,
    token      VARCHAR(255)                                                   NOT NULL,
    device_id  VARCHAR(100)                                                   NOT NULL,
    platform   VARCHAR(20)                                                    NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_notification_device_token_token UNIQUE (token),
    CONSTRAINT uq_notification_device_token_user_device UNIQUE (user_id, device_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
