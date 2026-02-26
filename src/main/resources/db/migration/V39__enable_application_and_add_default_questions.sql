UPDATE club
SET is_application_enabled = TRUE
WHERE is_application_enabled = FALSE OR is_application_enabled IS NULL;

INSERT INTO club_apply_question (club_id, question, is_required, created_at, updated_at)
SELECT c.id, '본인의 전화번호를 입력해주세요.', TRUE, NOW(), NOW()
FROM club c
WHERE NOT EXISTS (
    SELECT 1 FROM club_apply_question caq WHERE caq.club_id = c.id
);

INSERT INTO club_apply_question (club_id, question, is_required, created_at, updated_at)
SELECT c.id, '지원 동기', FALSE, NOW(), NOW()
FROM club c
WHERE (
    SELECT COUNT(*) FROM club_apply_question caq WHERE caq.club_id = c.id
) = 1
AND EXISTS (
    SELECT 1 FROM club_apply_question caq
    WHERE caq.club_id = c.id AND caq.question = '본인의 전화번호를 입력해주세요.'
);
