CREATE TABLE IF NOT EXISTS club_recruitment_image
(
    id                          INT          AUTO_INCREMENT PRIMARY KEY,
    club_recruitment_id         INT                                                                 NOT NULL,
    url                         VARCHAR(255)                                                        NOT NULL,
    display_order               INT          DEFAULT 0                                              NOT NULL,
    created_at                  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP                              NOT NULL,
    updated_at                  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  NOT NULL,

    FOREIGN KEY (club_recruitment_id) REFERENCES club_recruitment (id) ON DELETE CASCADE
);
