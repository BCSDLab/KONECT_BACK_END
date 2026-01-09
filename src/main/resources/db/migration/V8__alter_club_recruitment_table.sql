ALTER TABLE club_recruitment
    ADD COLUMN is_always_recruiting TINYINT(1) NULL DEFAULT 0 AFTER end_date;

ALTER TABLE club_recruitment
    MODIFY COLUMN start_date DATE NULL,
    MODIFY COLUMN end_date DATE NULL;
