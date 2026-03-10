-- Add unique constraint for (provider, oauth_email) on user_oauth_account table
-- This ensures that within the same provider, each email is associated with only one user
-- Required for safe OAuth login when providerId is NULL but email-based lookup is used

-- Step 1: Check for existing duplicates
SELECT provider, oauth_email, COUNT(*) AS cnt
FROM user_oauth_account
WHERE oauth_email IS NOT NULL
GROUP BY provider, oauth_email
HAVING cnt > 1;

DELETE dup
FROM user_oauth_account dup
JOIN (
    SELECT ranked.id
    FROM (
        SELECT
            uoa.id,
            ROW_NUMBER() OVER (
                PARTITION BY uoa.provider, uoa.oauth_email
                ORDER BY (u.deleted_at IS NULL) DESC, uoa.created_at DESC, uoa.id DESC
            ) AS rn
        FROM user_oauth_account uoa
        INNER JOIN users u ON u.id = uoa.user_id
        WHERE uoa.oauth_email IS NOT NULL
    ) ranked
    WHERE ranked.rn > 1
) to_delete ON dup.id = to_delete.id;

-- Step 3: Add unique constraint
ALTER TABLE user_oauth_account
ADD CONSTRAINT uq_user_oauth_account_provider_oauth_email
UNIQUE (provider, oauth_email);
