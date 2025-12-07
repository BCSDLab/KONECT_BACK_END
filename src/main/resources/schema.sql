CREATE TABLE users
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(100)                        NOT NULL,
    name           VARCHAR(50)                         NOT NULL,
    phone_number   VARCHAR(20) UNIQUE                  NOT NULL,
    student_number VARCHAR(20) UNIQUE                  NOT NULL,
    provider       ENUM('GOOGLE', 'KAKAO', 'NAVER')    NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL

    CONSTRAINT uq_email_provider UNIQUE (email, provider)
);

CREATE TABLE unregistered_user
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(255)                        NOT NULL,
    provider       ENUM('GOOGLE', 'KAKAO', 'NAVER')    NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL

    CONSTRAINT uq_email_provider UNIQUE (email, oauth_provider)
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
    id               INT AUTO_INCREMENT PRIMARY KEY,
    club_category_id INT                                 NOT NULL,
    name             VARCHAR(50)                         NOT NULL,
    description      VARCHAR(100)                        NOT NULL,
    introduce        TEXT                                NOT NULL,
    image_url        VARCHAR(255)                        NOT NULL,
    location         VARCHAR(255)                        NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

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

CREATE TABLE club_position_group
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255)                        NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE club_position
(
    id                     INT AUTO_INCREMENT,
    club_id                INT                                 NOT NULL,
    club_position_group_id INT                                 NOT NULL,
    name                   VARCHAR(255)                        NOT NULL,

    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (club_id, name),

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (club_position_group_id) REFERENCES club_position_group (id)
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
    club_id          INT                                 NOT NULL,
    user_id          INT                                 NOT NULL,
    club_position_id INT                                 NOT NULL,
    is_admin         BOOLEAN   DEFAULT FALSE             NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (club_id, user_id),

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (club_position_id) REFERENCES club_position (id)
);

CREATE TABLE club_representative
(
    club_id    INT                                 NOT NULL,
    user_id    INT                                 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (club_id, user_id),
    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE club_fee_payment
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    club_id       INT                                 NOT NULL,
    user_id       INT                                 NOT NULL,
    date          DATE                                NOT NULL,
    status        VARCHAR(255)                        NOT NULL,
    exempt_reason VARCHAR(255) NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (club_id, user_id, date),

    FOREIGN KEY (club_id, user_id) REFERENCES club_member (club_id, user_id) ON DELETE CASCADE
);

CREATE TABLE club_position_fee
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    club_position_id INT                                 NOT NULL,
    fee              INT                                 NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (club_position_id) REFERENCES club_position (id)
);

CREATE TABLE council
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255)                        NOT NULL,
    introduce    TEXT                                NOT NULL,
    location     VARCHAR(255)                        NOT NULL,
    phone_number VARCHAR(255)                        NOT NULL,
    email        VARCHAR(255)                        NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE council_operating_hour
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    council_id  INT                                 NOT NULL,
    day_of_week VARCHAR(20)                         NOT NULL,
    open_time   TIME,
    close_time  TIME,
    is_closed   BOOLEAN   DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (council_id, day_of_week),
    FOREIGN KEY (council_id) REFERENCES council (id) ON DELETE CASCADE
);

CREATE TABLE council_social_media
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    council_id    INT                                 NOT NULL,
    platform_name VARCHAR(50)                         NOT NULL,
    url           VARCHAR(500)                        NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (council_id, platform_name),
    FOREIGN KEY (council_id) REFERENCES council (id) ON DELETE CASCADE
);

CREATE TABLE council_notice
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    council_id INT                                 NOT NULL,
    title      VARCHAR(255)                        NOT NULL,
    content    TEXT                                NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (council_id) REFERENCES council (id) on DELETE CASCADE
);
