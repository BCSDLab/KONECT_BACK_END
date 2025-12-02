CREATE TABLE users
(
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    email               VARCHAR(100)                        NOT NULL UNIQUE,
    password            VARCHAR(255)                        NOT NULL,
    name                VARCHAR(50)                         NOT NULL,
    phone_number        VARCHAR(20)                         NOT NULL UNIQUE,
    student_number      VARCHAR(20)                         NOT NULL UNIQUE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE club_category
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255)                        NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE club_tag
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)                         NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE club
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    club_category_id   INT                                 NOT NULL,
    name               VARCHAR(50)                         NOT NULL,
    description        VARCHAR(100)                        NOT NULL,
    introduce          TEXT                                NOT NULL,
    image_url          VARCHAR(255)                        NOT NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (club_category_id) REFERENCES club_category (id)
);

CREATE TABLE club_tag_map
(
    club_id    INT                                 NOT NULL,
    tag_id     INT                                 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (club_id, tag_id),

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES club_tag (id) ON DELETE CASCADE
);

CREATE TABLE club_recruitment
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    club_id    INT                                 NOT NULL,
    start_date DATE                                NOT NULL,
    end_date   DATE                                NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (club_id),

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE
);

CREATE TABLE club_member
(
    club_id    INT                                 NOT NULL,
    user_id    INT                                 NOT NULL,
    is_admin   BOOLEAN   DEFAULT FALSE             NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (club_id, user_id),

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE club_representative
(
    club_id           INT                                 NOT NULL,
    user_id           INT                                 NOT NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (club_id, user_id),
    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE club_position_group
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255)                        NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE council_notice
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(255)                        NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);
