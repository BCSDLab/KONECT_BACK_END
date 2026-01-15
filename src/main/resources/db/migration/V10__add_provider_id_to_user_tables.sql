ALTER TABLE users
    ADD COLUMN provider_id VARCHAR(255) NULL AFTER provider,
    MODIFY COLUMN provider ENUM ('GOOGLE', 'KAKAO', 'NAVER', 'APPLE') NOT NULL,
    ADD CONSTRAINT uq_users_provider_provider_id UNIQUE (provider, provider_id);

ALTER TABLE unregistered_user
    ADD COLUMN provider_id VARCHAR(255) NULL AFTER provider,
    MODIFY COLUMN provider ENUM ('GOOGLE', 'KAKAO', 'NAVER', 'APPLE') NOT NULL,
    ADD CONSTRAINT uq_unregistered_user_provider_provider_id UNIQUE (provider, provider_id);
