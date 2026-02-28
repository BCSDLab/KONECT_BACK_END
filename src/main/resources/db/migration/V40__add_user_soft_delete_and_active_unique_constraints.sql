ALTER TABLE users
    ADD COLUMN deleted_at DATETIME NULL AFTER last_activity_at,
    ADD COLUMN active_flag INT GENERATED ALWAYS AS (
        CASE
            WHEN deleted_at IS NULL THEN 1
            ELSE NULL
        END
    ) STORED;

ALTER TABLE users
    DROP INDEX uq_users_phone_number,
    DROP INDEX uq_users_email_provider,
    DROP INDEX uq_users_university_id_student_number,
    DROP INDEX uq_users_provider_provider_id,
    ADD CONSTRAINT uq_users_phone_number_active UNIQUE (phone_number, active_flag),
    ADD CONSTRAINT uq_users_email_provider_active UNIQUE (email, provider, active_flag),
    ADD CONSTRAINT uq_users_university_id_student_number_active UNIQUE (university_id, student_number, active_flag),
    ADD CONSTRAINT uq_users_provider_provider_id_active UNIQUE (provider, provider_id, active_flag);
