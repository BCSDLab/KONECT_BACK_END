CREATE TABLE IF NOT EXISTS club_information_update_request
(
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    club_id               INT                                                           NOT NULL COMMENT '수정 요청 대상 동아리 ID',
    club_name             VARCHAR(50)                                                   NOT NULL COMMENT '요청 동아리 명',
    club_category         VARCHAR(255)                                                  NOT NULL COMMENT '요청 동아리 분과',
    short_description     VARCHAR(25)                                                   NOT NULL COMMENT '요청 한 줄 소개',
    image_url             VARCHAR(255)                                                  NOT NULL COMMENT '요청 동아리 로고 이미지 URL',
    location              VARCHAR(255)                                                  NOT NULL COMMENT '요청 동아리 위치',
    full_introduction     TEXT                                                          NOT NULL COMMENT '요청 동아리 소개',
    status                VARCHAR(20)  DEFAULT 'PENDING'                                NOT NULL COMMENT '요청 상태 (PENDING, APPROVED, REJECTED)',
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (club_id) REFERENCES club (id)
) COMMENT '동아리 정보 수정 요청';
