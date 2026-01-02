CREATE TABLE IF NOT EXISTS study_timer
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT                                 NOT NULL,
    started_at TIMESTAMP                           NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (user_id),

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS study_time_daily
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT                                 NOT NULL,
    study_date    DATE                                NOT NULL,
    total_seconds BIGINT                              NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (user_id, study_date),

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS study_time_monthly
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT                                 NOT NULL,
    study_month   DATE                                NOT NULL,
    total_seconds BIGINT                              NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (user_id, study_month),

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS study_time_total
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT                                 NOT NULL,
    total_seconds BIGINT                              NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (user_id),

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
