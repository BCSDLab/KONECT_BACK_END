ALTER TABLE notification_inbox
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE notification_inbox
    ADD INDEX idx_notification_inbox_user_id_created_at (user_id, created_at DESC);

ALTER TABLE notification_inbox
    DROP INDEX idx_notification_inbox_user_id;
