CREATE TABLE club_registration_request
(
    id              INT AUTO_INCREMENT PRIMARY KEY,
    university_name VARCHAR(50)  NOT NULL,
    club_name       VARCHAR(50)  NOT NULL,
    club_category   VARCHAR(20)  NOT NULL,
    topic           VARCHAR(20)  NOT NULL,
    emoji           VARCHAR(20)  NOT NULL,
    description     VARCHAR(30)  NOT NULL,
    introduce       TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE club_registration_request_media
(
    id                           INT AUTO_INCREMENT PRIMARY KEY,
    club_registration_request_id INT          NOT NULL,
    url                          VARCHAR(255) NOT NULL,
    display_order                INT          NOT NULL,
    created_at                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_registration_request_media_request
        FOREIGN KEY (club_registration_request_id)
            REFERENCES club_registration_request (id)
            ON DELETE CASCADE
);
