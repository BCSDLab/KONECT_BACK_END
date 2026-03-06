-- users 테이블의 OAuth 관련 unique 제약조건 삭제
ALTER TABLE users DROP INDEX uq_users_email_provider_active;
ALTER TABLE users DROP INDEX uq_users_provider_provider_id_active;

-- users 테이블에서 OAuth 컬럼 삭제
ALTER TABLE users DROP COLUMN provider;
ALTER TABLE users DROP COLUMN provider_id;
ALTER TABLE users DROP COLUMN apple_refresh_token;
