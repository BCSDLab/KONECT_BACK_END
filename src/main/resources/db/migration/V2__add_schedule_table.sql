CREATE TABLE IF NOT EXISTS schedule
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(255)                        NOT NULL,
    started_at    TIMESTAMP                           NOT NULL,
    ended_at      TIMESTAMP                           NOT NULL,
    schedule_type VARCHAR(255)                        NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);
