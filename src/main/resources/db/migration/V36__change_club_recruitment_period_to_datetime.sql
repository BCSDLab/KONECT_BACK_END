ALTER TABLE club_recruitment
    MODIFY COLUMN start_date DATETIME NULL,
    MODIFY COLUMN end_date DATETIME NULL;

UPDATE club_recruitment
SET start_date = TIMESTAMP(start_date, '00:00:00')
WHERE start_date IS NOT NULL;

UPDATE club_recruitment
SET end_date = TIMESTAMP(end_date, '23:59:59')
WHERE end_date IS NOT NULL;
