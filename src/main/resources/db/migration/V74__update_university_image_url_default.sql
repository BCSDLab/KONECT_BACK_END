ALTER TABLE university
    MODIFY COLUMN image_url VARCHAR(255) NOT NULL DEFAULT 'https://stage-static.koreatech.in/konect/university/university_logo_sample.webp' AFTER region;

UPDATE university
SET image_url = 'https://stage-static.koreatech.in/konect/university/university_logo_sample.webp'
WHERE image_url IS NULL
   OR image_url = 'https://stage-static.koreatech.in/konect/user/university_logo_sample.png';
