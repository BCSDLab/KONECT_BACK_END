CREATE TABLE study_time_ranking
(
    ranking_type    ENUM('PERSONAL','STUDENT_NUMBER','CLUB') NOT NULL,
    target_id       VARCHAR(20) NOT NULL,
    target_name     VARCHAR(100),
    daily_seconds   BIGINT      NOT NULL,
    monthly_seconds BIGINT      NOT NULL,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (ranking_type, target_id)
);

CREATE INDEX idx_daily
    ON study_time_ranking (ranking_type, daily_seconds DESC, monthly_seconds DESC);

CREATE INDEX idx_monthly
    ON study_time_ranking (ranking_type, monthly_seconds DESC, daily_seconds DESC);
