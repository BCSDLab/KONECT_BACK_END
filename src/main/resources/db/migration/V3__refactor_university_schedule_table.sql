ALTER TABLE university_schedule
    DROP COLUMN title,
    DROP COLUMN started_at,
    DROP COLUMN ended_at;

ALTER TABLE university_schedule
    MODIFY COLUMN id INT NOT NULL;

ALTER TABLE university_schedule
    ADD CONSTRAINT FOREIGN KEY (id) REFERENCES schedule (id) ON DELETE CASCADE;
