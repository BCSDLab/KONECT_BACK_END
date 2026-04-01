CREATE TABLE notification_inbox
(
    id         INT          NOT NULL AUTO_INCREMENT,
    user_id    INT          NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    title      VARCHAR(100) NOT NULL,
    body       VARCHAR(300) NOT NULL,
    path       VARCHAR(200),
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notification_inbox_user_id (user_id),
    INDEX idx_notification_inbox_user_id_is_read (user_id, is_read),
    CONSTRAINT fk_notification_inbox_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
