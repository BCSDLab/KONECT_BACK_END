CREATE TABLE IF NOT EXISTS fcm_device_token
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT                                                            NOT NULL,
    token      VARCHAR(255)                                                   NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_fcm_device_token_token UNIQUE (token),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
