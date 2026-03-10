ALTER TABLE users
    ADD INDEX idx_users_university_id (university_id),
    DROP INDEX uq_users_university_id_student_number_active;
