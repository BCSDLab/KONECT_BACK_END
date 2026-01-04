CREATE TABLE IF NOT EXISTS withdrawn_users
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(100)                                                   NOT NULL,
    provider         ENUM ('GOOGLE', 'KAKAO', 'NAVER')                              NOT NULL,
    original_user_id INT                                                            NOT NULL,
    withdrawn_at     TIMESTAMP                                                      NOT NULL,
    university_id    INT,
    student_number   VARCHAR(20),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_withdrawn_users_email_provider UNIQUE (email, provider),
    INDEX idx_withdrawn_at (withdrawn_at)
);
