ALTER TABLE user_oauth_account
DROP FOREIGN KEY fk_user_oauth_account_user;

ALTER TABLE user_oauth_account
ADD CONSTRAINT fk_user_oauth_account_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE;
