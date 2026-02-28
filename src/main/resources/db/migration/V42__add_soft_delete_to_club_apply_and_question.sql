ALTER TABLE club_apply
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER fee_payment_image_url,
    ADD COLUMN pending_flag INT GENERATED ALWAYS AS (
        CASE
            WHEN status = 'PENDING' THEN 1
            ELSE NULL
        END
    ) STORED;

ALTER TABLE club_apply
    DROP INDEX uq_club_apply_club_id_user_id,
    ADD CONSTRAINT uq_club_apply_club_id_user_id_pending UNIQUE (club_id, user_id, pending_flag);

ALTER TABLE club_apply_question
    ADD COLUMN deleted_at DATETIME NULL AFTER is_required;
