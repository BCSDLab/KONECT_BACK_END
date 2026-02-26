ALTER TABLE users
    ADD COLUMN last_login_at DATETIME NULL,
    ADD COLUMN last_activity_at DATETIME NULL;
