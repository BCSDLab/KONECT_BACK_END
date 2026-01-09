ALTER TABLE club_apply_question
    ADD COLUMN question_order INT;

SET @current_club_id := NULL;
SET @row_number := 0;

UPDATE club_apply_question
SET question_order = (CASE
    WHEN @current_club_id = club_id THEN @row_number := @row_number + 1
    ELSE @row_number := 1
END),
    @current_club_id := club_id
ORDER BY club_id, id;

ALTER TABLE club_apply_question
    MODIFY question_order INT NOT NULL;

ALTER TABLE club_apply_question
    ADD CONSTRAINT uq_club_apply_question_club_id_question_order
        UNIQUE (club_id, question_order);
