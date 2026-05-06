CREATE TABLE IF NOT EXISTS event
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(100)                                                   NOT NULL,
    subtitle         VARCHAR(255),
    poster_image_url VARCHAR(255),
    notice           TEXT,
    start_at         TIMESTAMP                                                      NOT NULL,
    end_at           TIMESTAMP                                                      NOT NULL,
    status           ENUM ('DRAFT', 'PUBLISHED', 'ENDED')                           NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS event_program
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    event_id      INT                                                            NOT NULL,
    type          ENUM ('POINT', 'RESONANCE')                                    NOT NULL,
    title         VARCHAR(100)                                                   NOT NULL,
    description   VARCHAR(255)                                                   NOT NULL,
    thumbnail_url VARCHAR(255),
    reward_point  INT,
    status        ENUM ('UPCOMING', 'ONGOING', 'ENDED')                          NOT NULL,
    display_order INT                                                            NOT NULL DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);
