-- Restores web_university and web_club from backup tables created by web_club_import_mapped_8.sql.
-- Run this only after confirming the backup tables exist and contain the desired previous state.

START TRANSACTION;

DELETE FROM web_club;
DELETE FROM web_university;

INSERT INTO web_university SELECT * FROM web_university_backup_before_mapped_8;
INSERT INTO web_club SELECT * FROM web_club_backup_before_mapped_8;

SELECT
    (SELECT COUNT(*) FROM web_university) AS restored_web_university_count,
    (SELECT COUNT(*) FROM web_club) AS restored_web_club_count;

COMMIT;
