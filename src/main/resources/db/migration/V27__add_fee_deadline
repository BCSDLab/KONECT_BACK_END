ALTER TABLE club_recruitment ADD COLUMN is_fee_required TINYINT(1) NULL;

UPDATE club_recruitment cr
    INNER JOIN club c ON cr.club_id = c.id
SET cr.is_fee_required = c.is_fee_required;

ALTER TABLE club DROP COLUMN is_fee_required;

ALTER TABLE club ADD COLUMN fee_deadline DATE NULL;
