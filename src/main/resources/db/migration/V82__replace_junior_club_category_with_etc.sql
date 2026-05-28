-- Replace the retired JUNIOR category with ETC.
-- Backup recommendation before production deploy:
--   mysqldump <database> club web_club club_registration_request club_information_update_request > backup_before_v82.sql
-- Expected impact: all rows that still use JUNIOR across club-related tables.

SELECT 'before' AS phase, 'club' AS table_name, COUNT(*) AS junior_count
FROM club
WHERE club_category = 'JUNIOR';

SELECT 'before' AS phase, 'web_club' AS table_name, COUNT(*) AS junior_count
FROM web_club
WHERE club_category = 'JUNIOR';

SELECT 'before' AS phase, 'club_registration_request' AS table_name, COUNT(*) AS junior_count
FROM club_registration_request
WHERE club_category = 'JUNIOR';

SELECT 'before' AS phase, 'club_information_update_request' AS table_name, COUNT(*) AS junior_count
FROM club_information_update_request
WHERE club_category = 'JUNIOR';

UPDATE club
SET club_category = 'ETC',
    updated_at = updated_at
WHERE club_category = 'JUNIOR';

UPDATE web_club
SET club_category = 'ETC',
    updated_at = updated_at
WHERE club_category = 'JUNIOR';

UPDATE club_registration_request
SET club_category = 'ETC',
    updated_at = updated_at
WHERE club_category = 'JUNIOR';

UPDATE club_information_update_request
SET club_category = 'ETC',
    updated_at = updated_at
WHERE club_category = 'JUNIOR';

SELECT 'after' AS phase, 'club' AS table_name, COUNT(*) AS junior_count
FROM club
WHERE club_category = 'JUNIOR';

SELECT 'after' AS phase, 'web_club' AS table_name, COUNT(*) AS junior_count
FROM web_club
WHERE club_category = 'JUNIOR';

SELECT 'after' AS phase, 'club_registration_request' AS table_name, COUNT(*) AS junior_count
FROM club_registration_request
WHERE club_category = 'JUNIOR';

SELECT 'after' AS phase, 'club_information_update_request' AS table_name, COUNT(*) AS junior_count
FROM club_information_update_request
WHERE club_category = 'JUNIOR';
