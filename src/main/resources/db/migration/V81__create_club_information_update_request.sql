CREATE TABLE IF NOT EXISTS club_information_update_request
(
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    web_club_id           INT                                                           NOT NULL COMMENT '수정 요청 대상 웹 동아리 ID',
    university_name       VARCHAR(255)                                                  NOT NULL COMMENT '요청 대학교 명',
    club_name             VARCHAR(50)                                                   NOT NULL COMMENT '요청 동아리 명',
    club_category         VARCHAR(255)                                                  NOT NULL COMMENT '요청 동아리 분과',
    club_topic            VARCHAR(20)                                                   NOT NULL COMMENT '요청 동아리 주제',
    club_emoji            VARCHAR(10)                                                   NOT NULL COMMENT '요청 동아리 이모지',
    short_description     VARCHAR(30)                                                   NOT NULL COMMENT '요청 한 줄 소개',
    full_introduction     TEXT                                                          NOT NULL COMMENT '요청 동아리 소개',
    status                VARCHAR(20)  DEFAULT 'PENDING'                                NOT NULL COMMENT '요청 상태 (PENDING, APPROVED, REJECTED)',
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (web_club_id) REFERENCES web_club (id)
) COMMENT '동아리 정보 수정 요청';

CREATE TABLE IF NOT EXISTS club_information_update_request_image
(
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    request_id            INT                                                           NOT NULL COMMENT '동아리 정보 수정 요청 ID',
    image_url             VARCHAR(500)                                                  NOT NULL COMMENT '이미지 URL',
    display_order         INT         DEFAULT 0                                         NOT NULL COMMENT '표시 순서',
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_club_information_update_request_image_order UNIQUE (request_id, display_order),
    FOREIGN KEY (request_id) REFERENCES club_information_update_request (id) ON DELETE CASCADE
) COMMENT '동아리 정보 수정 요청 이미지';
