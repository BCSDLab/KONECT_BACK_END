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

CREATE TABLE IF NOT EXISTS event_booth
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    event_id       INT                                                            NOT NULL,
    name           VARCHAR(100)                                                   NOT NULL,
    category       VARCHAR(50)                                                    NOT NULL,
    description    TEXT,
    location_label VARCHAR(100),
    zone           VARCHAR(50),
    thumbnail_url  VARCHAR(255),
    is_open        BOOLEAN                                                        NOT NULL DEFAULT true,
    display_order  INT                                                            NOT NULL DEFAULT 0,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS event_booth_map
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    event_id      INT                                                            NOT NULL,
    map_image_url VARCHAR(255),
    width         INT,
    height        INT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE,
    CONSTRAINT uq_event_booth_map_event_id UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS event_booth_map_item
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    event_booth_map_id INT                                                            NOT NULL,
    event_booth_id     INT                                                            NOT NULL,
    x                  INT                                                            NOT NULL,
    y                  INT                                                            NOT NULL,
    width              INT                                                            NOT NULL,
    height             INT                                                            NOT NULL,
    status             ENUM ('OPEN', 'CLOSED', 'HIDDEN')                             NOT NULL DEFAULT 'OPEN',
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (event_booth_map_id) REFERENCES event_booth_map (id) ON DELETE CASCADE,
    FOREIGN KEY (event_booth_id) REFERENCES event_booth (id) ON DELETE CASCADE,
    CONSTRAINT uq_event_booth_map_item_booth_id UNIQUE (event_booth_id)
);

CREATE TABLE IF NOT EXISTS event_mini_event
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    event_id      INT                                                            NOT NULL,
    title         VARCHAR(100)                                                   NOT NULL,
    description   VARCHAR(255)                                                   NOT NULL,
    thumbnail_url VARCHAR(255),
    reward_label  VARCHAR(100),
    status        ENUM ('UPCOMING', 'ONGOING', 'ENDED')                          NOT NULL,
    display_order INT                                                            NOT NULL DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS event_content
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    event_id      INT                                                            NOT NULL,
    title         VARCHAR(100)                                                   NOT NULL,
    summary       VARCHAR(255)                                                   NOT NULL,
    body          TEXT,
    thumbnail_url VARCHAR(255),
    type          ENUM ('ARTICLE', 'IMAGE', 'VIDEO')                             NOT NULL,
    published_at  TIMESTAMP,
    display_order INT                                                            NOT NULL DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);
