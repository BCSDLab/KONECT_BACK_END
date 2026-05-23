CREATE TABLE IF NOT EXISTS club_registration_request
(
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    university_name       VARCHAR(255)                                                  NOT NULL COMMENT '대학교 명',
    club_name             VARCHAR(50)                                                   NOT NULL COMMENT '동아리 명',
    club_category         VARCHAR(255)                                                  NOT NULL COMMENT '동아리 분과',
    club_topic            VARCHAR(20)                                                   NOT NULL COMMENT '동아리 주제',
    club_emoji            VARCHAR(10)                                                   NOT NULL COMMENT '동아리 이모지',
    short_description     VARCHAR(30)                                                   NOT NULL COMMENT '한 줄 소개',
    full_introduction     TEXT                                                          NOT NULL COMMENT '동아리 소개 (2000자)',
    image_urls            JSON                                                          NULL COMMENT '사진 및 영상 URL 목록 (최대 5개)',
    status                VARCHAR(20)  DEFAULT 'PENDING'                                NOT NULL COMMENT '요청 상태 (PENDING, APPROVED, REJECTED)',
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
) COMMENT '동아리 등록 요청';
