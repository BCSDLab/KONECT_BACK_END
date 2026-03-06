-- user_oauth_account 테이블에 apple_refresh_token 컬럼 추가
ALTER TABLE user_oauth_account
ADD COLUMN apple_refresh_token VARCHAR(1024) NULL AFTER oauth_email;

-- users 테이블에서 user_oauth_account 테이블로 apple_refresh_token 데이터 마이그레이션
-- V43에서 이미 provider, provider_id는 마이그레이션됨
UPDATE user_oauth_account uoa
INNER JOIN users u
  ON uoa.user_id = u.id
  AND uoa.provider = u.provider
SET uoa.apple_refresh_token = u.apple_refresh_token
WHERE u.apple_refresh_token IS NOT NULL;

-- V43에서 deleted_at IS NULL 조건으로 누락된 탈퇴 유저 데이터도 보정
INSERT INTO user_oauth_account (user_id, provider, provider_id, oauth_email, apple_refresh_token, created_at, updated_at)
SELECT u.id, u.provider, u.provider_id, u.email, u.apple_refresh_token, NOW(), NOW()
FROM users u
WHERE u.provider IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM user_oauth_account ua
    WHERE ua.user_id = u.id AND ua.provider = u.provider
  );
