ALTER TABLE university_schedule
    DROP COLUMN IF EXISTS title,
    DROP COLUMN IF EXISTS started_at,
    DROP COLUMN IF EXISTS ended_at;

ALTER TABLE university_schedule
    MODIFY COLUMN id INT NOT NULL;

ALTER TABLE university_schedule
    ADD CONSTRAINT FOREIGN KEY (id) REFERENCES schedule (id) ON DELETE CASCADE;
