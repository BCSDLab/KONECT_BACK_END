CREATE TABLE user_oauth_account (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    oauth_email VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_user_oauth_account_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT uq_user_oauth_account_provider_provider_id
        UNIQUE (provider, provider_id),

    CONSTRAINT uq_user_oauth_account_user_provider
        UNIQUE (user_id, provider)
);

INSERT INTO user_oauth_account (user_id, provider, provider_id, oauth_email, created_at, updated_at)
SELECT u.id,
       u.provider,
       u.provider_id,
       u.email,
       NOW(),
       NOW()
FROM users u
WHERE u.deleted_at IS NULL
  AND u.provider IS NOT NULL
  AND u.provider_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM user_oauth_account ua
    WHERE ua.provider = u.provider
      AND ua.provider_id = u.provider_id
);
