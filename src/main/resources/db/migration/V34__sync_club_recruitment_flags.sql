UPDATE club c
SET c.is_recruitment_enabled = CASE
    WHEN EXISTS (SELECT 1 FROM club_recruitment cr WHERE cr.club_id = c.id)
    THEN 1 ELSE 0
END;

UPDATE club c
SET c.is_application_enabled = CASE
    WHEN EXISTS (SELECT 1 FROM club_recruitment cr WHERE cr.club_id = c.id)
    THEN 1 ELSE 0
END;

UPDATE club c
LEFT JOIN club_recruitment cr ON cr.club_id = c.id
SET c.is_fee_required = COALESCE(cr.is_fee_required, 0);
