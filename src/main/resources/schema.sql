CREATE TABLE IF NOT EXISTS university
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    korean_name VARCHAR(255)                        NOT NULL,
    campus      VARCHAR(255)                        NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_university_korean_name_campus UNIQUE (korean_name, campus)
);

CREATE TABLE IF NOT EXISTS users
(
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    university_id          INT                                 NOT NULL,
    email                  VARCHAR(100)                        NOT NULL,
    name                   VARCHAR(30)                         NOT NULL,
    phone_number           VARCHAR(20),
    student_number         VARCHAR(20)                         NOT NULL,
    provider               ENUM ('GOOGLE', 'KAKAO', 'NAVER')   NOT NULL,
    is_marketing_agreement BOOLEAN                             NOT NULL,
    image_url              VARCHAR(255)                        NOT NULL,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (university_id) REFERENCES university (id),
    CONSTRAINT uq_users_phone_number UNIQUE (phone_number),
    CONSTRAINT uq_users_email_provider UNIQUE (email, provider),
    CONSTRAINT uq_users_university_id_student_number UNIQUE (university_id, student_number)
);

CREATE TABLE IF NOT EXISTS unregistered_user
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255)                        NOT NULL,
    provider   ENUM ('GOOGLE', 'KAKAO', 'NAVER')   NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_unregistered_user_email_provider UNIQUE (email, provider)
);

CREATE TABLE IF NOT EXISTS club_tag
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)                         NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_club_tag_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS club
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    university_id      INT                                 NOT NULL,
    club_category      VARCHAR(255)                        NOT NULL,
    name               VARCHAR(50)                         NOT NULL,
    description        VARCHAR(100)                        NOT NULL,
    introduce          TEXT                                NOT NULL,
    image_url          VARCHAR(255)                        NOT NULL,
    location           VARCHAR(255)                        NOT NULL,
    fee_amount         INT,
    fee_bank           VARCHAR(100),
    fee_account_number VARCHAR(100),
    fee_account_holder VARCHAR(100),
    fee_deadline       DATE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (university_id) REFERENCES university (id)
);

CREATE TABLE IF NOT EXISTS club_apply_question
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    club_id     INT                                 NOT NULL,
    question    VARCHAR(255)                        NOT NULL,
    is_required BOOLEAN                             NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS club_apply
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    club_id    INT                                 NOT NULL,
    user_id    INT                                 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_club_apply_club_id_user_id UNIQUE (club_id, user_id)
);

CREATE TABLE IF NOT EXISTS club_apply_answer
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    apply_id    INT                                 NOT NULL,
    question_id INT                                 NOT NULL,
    answer      TEXT                                NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (apply_id) REFERENCES club_apply (id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES club_apply_question (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS club_tag_map
(
    club_id    INT                                 NOT NULL,
    tag_id     INT                                 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (club_id, tag_id),
    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES club_tag (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS club_position
(
    id                  INT AUTO_INCREMENT,
    club_id             INT                                 NOT NULL,
    name                VARCHAR(255)                        NOT NULL,
    club_position_group VARCHAR(255)                        NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    CONSTRAINT uq_club_position_club_id_name UNIQUE (club_id, name)
);

CREATE TABLE IF NOT EXISTS club_recruitment
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    club_id    INT                                 NOT NULL,
    start_date DATE                                NOT NULL,
    end_date   DATE                                NOT NULL,
    content    TEXT                                NOT NULL,
    image_url  VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    CONSTRAINT uq_club_recruitment_club_id UNIQUE (club_id)
);

CREATE TABLE IF NOT EXISTS club_member
(
    club_id          INT                                 NOT NULL,
    user_id          INT                                 NOT NULL,
    club_position_id INT                                 NOT NULL,
    is_fee_paid      BOOLEAN                             NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (club_id, user_id),
    FOREIGN KEY (club_id) REFERENCES club (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (club_position_id) REFERENCES club_position (id)
);

CREATE TABLE IF NOT EXISTS council
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    university_id  INT                                 NOT NULL,
    name           VARCHAR(255)                        NOT NULL,
    image_url      VARCHAR(255)                        NOT NULL,
    introduce      TEXT                                NOT NULL,
    personal_color VARCHAR(255)                        NOT NULL,
    location       VARCHAR(255)                        NOT NULL,
    phone_number   VARCHAR(255)                        NOT NULL,
    email          VARCHAR(255)                        NOT NULL,
    instagram_url  VARCHAR(255)                        NOT NULL,
    operating_hour VARCHAR(255)                        NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (university_id) REFERENCES university (id)
);

CREATE TABLE IF NOT EXISTS council_notice
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    council_id INT                                 NOT NULL,
    title      VARCHAR(255)                        NOT NULL,
    content    TEXT                                NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (council_id) REFERENCES council (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS council_notice_read_history
(
    id                INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT                                 NOT NULL,
    council_notice_id INT                                 NOT NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id) on DELETE CASCADE,
    FOREIGN KEY (council_notice_id) REFERENCES council_notice (id) on DELETE CASCADE,
    CONSTRAINT uq_council_notice_read_history_user_id_council_notice_id UNIQUE (user_id, council_notice_id)
);

CREATE TABLE IF NOT EXISTS university_schedule
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    university_id INT                                 NOT NULL,
    title         VARCHAR(255)                        NOT NULL,
    started_at    TIMESTAMP                           NOT NULL,
    ended_at      TIMESTAMP                           NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (university_id) REFERENCES university (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_room
(
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    sender_id            INT,
    receiver_id          INT,
    last_message_content TEXT NULL,
    last_message_sent_at TIMESTAMP NULL,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_message
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id INT                                 NOT NULL,
    sender_id    INT                                 NOT NULL,
    receiver_id  INT                                 NOT NULL,
    content      TEXT                                NOT NULL,
    is_read      BOOLEAN DEFAULT FALSE               NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (chat_room_id) REFERENCES chat_room (id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE
);
