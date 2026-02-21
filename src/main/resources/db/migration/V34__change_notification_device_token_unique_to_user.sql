DELETE ndt1
FROM notification_device_token ndt1
    INNER JOIN notification_device_token ndt2
        ON ndt1.user_id = ndt2.user_id
        AND (
            ndt1.updated_at < ndt2.updated_at
            OR (ndt1.updated_at = ndt2.updated_at AND ndt1.id < ndt2.id)
        );

ALTER TABLE notification_device_token
    DROP INDEX uq_notification_device_token_token,
    ADD CONSTRAINT uq_notification_device_token_user_id UNIQUE (user_id);
