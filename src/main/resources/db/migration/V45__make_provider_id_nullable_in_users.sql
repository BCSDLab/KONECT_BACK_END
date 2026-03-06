-- Make provider_id nullable in users table to support OAuth accounts without providerId
-- This migration is needed because V43 already ran, so we add this between V44 and V45

-- Step 1: Drop unique constraint that includes provider_id
ALTER TABLE users DROP INDEX uq_users_provider_provider_id_active;

-- Step 2: Make provider_id nullable
ALTER TABLE users
MODIFY COLUMN provider_id VARCHAR(255) NULL;
