UPDATE club
SET description = LEFT(description, 30)
WHERE CHAR_LENGTH(description) > 30;

ALTER TABLE club
    MODIFY description VARCHAR(30) NOT NULL;
