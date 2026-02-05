ALTER TABLE unregistered_user
    ADD COLUMN phone_number VARCHAR(20) NULL AFTER provider_id;
