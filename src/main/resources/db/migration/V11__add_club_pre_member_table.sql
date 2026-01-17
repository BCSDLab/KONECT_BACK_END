CREATE TABLE IF NOT EXISTS club_pre_member
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    club_id        INT                                                            NOT NULL,
    student_number VARCHAR(20)                                                    NOT NULL,
    name           VARCHAR(30)                                                    NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    CONSTRAINT uq_club_pre_member_club_id_student_number_name UNIQUE (club_id, student_number, name),
    INDEX idx_club_pre_member_student_number_name (student_number, name)
);
