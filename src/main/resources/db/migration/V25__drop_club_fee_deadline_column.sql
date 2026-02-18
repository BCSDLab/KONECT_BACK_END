ALTER TABLE club DROP COLUMN fee_deadline;

ALTER TABLE club ADD COLUMN is_fee_required TINYINT(1) NULL;

UPDATE club c
    INNER JOIN club_recruitment cr ON c.id = cr.club_id
SET c.is_fee_required = cr.is_fee_required;

ALTER TABLE club_recruitment DROP COLUMN is_fee_required;
