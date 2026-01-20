ALTER TABLE council
    CHANGE instagram_url instagram_user_name VARCHAR(255) NOT NULL;

UPDATE council
SET instagram_user_name = CASE
    WHEN instagram_user_name IS NULL THEN instagram_user_name
    WHEN instagram_user_name LIKE '%/%' OR instagram_user_name LIKE 'http%' THEN
        SUBSTRING_INDEX(
            SUBSTRING_INDEX(TRIM(TRAILING '/' FROM instagram_user_name), '?', 1),
            '/',
            -1
        )
    ELSE instagram_user_name
END;

UPDATE council
SET instagram_user_name = SUBSTRING(instagram_user_name, 2)
WHERE instagram_user_name LIKE '@%';
