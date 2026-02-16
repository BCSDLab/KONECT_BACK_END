ALTER TABLE users ADD COLUMN apple_refresh_token VARCHAR(1024) NULL;
ALTER TABLE unregistered_user ADD COLUMN apple_refresh_token VARCHAR(1024) NULL;
