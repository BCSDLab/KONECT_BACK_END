INSERT INTO club_recruitment_image (club_recruitment_id, url, display_order, created_at, updated_at)
SELECT
    id,
    image_url,
    0,
    created_at,
    updated_at
FROM club_recruitment
WHERE image_url IS NOT NULL
  AND image_url != '';

ALTER TABLE club_recruitment
DROP COLUMN image_url;
