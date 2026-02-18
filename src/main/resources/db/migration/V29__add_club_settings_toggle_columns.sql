ALTER TABLE club ADD COLUMN is_recruitment_enabled TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE club ADD COLUMN is_application_enabled TINYINT(1) NOT NULL DEFAULT 1;

UPDATE club SET is_recruitment_enabled = 1 WHERE is_recruitment_enabled IS NULL;
UPDATE club SET is_application_enabled = 1 WHERE is_application_enabled IS NULL;
