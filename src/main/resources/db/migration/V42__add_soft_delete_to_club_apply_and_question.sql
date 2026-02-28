ALTER TABLE club_apply
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER fee_payment_image_url,
    ADD COLUMN state INT GENERATED ALWAYS AS (
        CASE
            WHEN status = 'PENDING' THEN 1
            ELSE NULL
        END
    ) STORED;

UPDATE club_apply ca
SET ca.status = 'APPROVED'
WHERE EXISTS (
    SELECT 1
    FROM club_member cm
    WHERE cm.club_id = ca.club_id
      AND cm.user_id = ca.user_id
);

ALTER TABLE club_apply
    DROP INDEX uq_club_apply_club_id_user_id,
    ADD CONSTRAINT uq_club_apply_club_id_user_id_state UNIQUE (club_id, user_id, state);

ALTER TABLE club_apply_question
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER is_required;
