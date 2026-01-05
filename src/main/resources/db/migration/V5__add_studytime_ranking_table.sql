CREATE TABLE IF NOT EXISTS ranking_type
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)                        NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

INSERT INTO ranking_type (id, name)
VALUES (1, 'CLUB'),
       (2, 'STUDENT_NUMBER'),
       (3, 'PERSONAL');

CREATE TABLE IF NOT EXISTS study_time_ranking
(
    ranking_type_id INT                                 NOT NULL,
    target_id       INT                                 NOT NULL,
    target_name     VARCHAR(100)                        NOT NULL,
    daily_seconds   BIGINT                              NOT NULL,
    monthly_seconds BIGINT                              NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (ranking_type_id, target_id),
    FOREIGN KEY (ranking_type_id) REFERENCES ranking_type (id) ON DELETE CASCADE
);
