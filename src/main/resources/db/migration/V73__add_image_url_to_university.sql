ALTER TABLE university
    ADD COLUMN image_url VARCHAR(255) NULL AFTER region;

UPDATE university
SET image_url = ''
WHERE image_url IS NULL;

ALTER TABLE university
    MODIFY COLUMN image_url VARCHAR(255) NOT NULL AFTER region;
