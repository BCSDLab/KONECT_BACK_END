SET @has_column := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_apply_question'
      AND COLUMN_NAME = 'question_order'
);

SET @add_column_sql := IF(
    @has_column = 0,
    'ALTER TABLE club_apply_question ADD COLUMN question_order INT',
    'SELECT 1'
);

PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

SET @current_club_id := NULL;
SET @row_number := 0;

UPDATE club_apply_question q
JOIN (
    SELECT id,
           @row_number := IF(@current_club_id = club_id, @row_number + 1, 1) AS rn,
           @current_club_id := club_id AS current_club
    FROM club_apply_question
    ORDER BY club_id, id
) t ON q.id = t.id
SET q.question_order = t.rn;

ALTER TABLE club_apply_question
    MODIFY question_order INT NOT NULL;

SET @has_constraint := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_apply_question'
      AND CONSTRAINT_NAME = 'uq_club_apply_question_club_id_question_order'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);

SET @add_constraint_sql := IF(
    @has_constraint = 0,
    'ALTER TABLE club_apply_question ADD CONSTRAINT uq_club_apply_question_club_id_question_order UNIQUE (club_id, question_order)',
    'SELECT 1'
);

PREPARE add_constraint_stmt FROM @add_constraint_sql;
EXECUTE add_constraint_stmt;
DEALLOCATE PREPARE add_constraint_stmt;
