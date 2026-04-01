ALTER TABLE notification_inbox
    DROP INDEX idx_notification_inbox_user_id_created_at;

ALTER TABLE notification_inbox
    ADD INDEX idx_notification_inbox_user_id_created_at_id (user_id, created_at DESC, id DESC);
