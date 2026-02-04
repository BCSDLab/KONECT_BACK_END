ALTER TABLE club_member
    ADD COLUMN club_position VARCHAR(20) NULL;

UPDATE club_member cm
    INNER JOIN club_position cp ON cm.club_position_id = cp.id
    SET cm.club_position = cp.club_position_group;

ALTER TABLE club_member
    MODIFY COLUMN club_position VARCHAR(20) NOT NULL;

DROP PROCEDURE IF EXISTS drop_fk_club_position_id;

DELIMITER //
CREATE PROCEDURE drop_fk_club_position_id()
BEGIN
    DECLARE fk_name VARCHAR(255);

SELECT CONSTRAINT_NAME INTO fk_name
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'club_member'
  AND COLUMN_NAME = 'club_position_id'
  AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1;

IF fk_name IS NOT NULL THEN
        SET @sql = CONCAT('ALTER TABLE club_member DROP FOREIGN KEY ', fk_name);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END IF;
END //
DELIMITER ;

CALL drop_fk_club_position_id();
DROP PROCEDURE IF EXISTS drop_fk_club_position_id;

ALTER TABLE club_member
DROP COLUMN club_position_id;

DROP TABLE IF EXISTS club_position;
