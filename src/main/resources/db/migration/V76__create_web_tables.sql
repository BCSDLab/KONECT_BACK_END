-- 웹용 university 테이블 생성 (앱용 university 테이블 구조 복사)
CREATE TABLE IF NOT EXISTS web_university
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    korean_name VARCHAR(255)                                                   NOT NULL,
    campus      VARCHAR(255)                                                   NOT NULL,
    region      VARCHAR(50)                                                    NOT NULL DEFAULT 'UNKNOWN',
    image_url   VARCHAR(255)                                                   NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_web_university_korean_name_campus UNIQUE (korean_name, campus)
);

-- 앱용 university 데이터를 웹용 테이블로 복사
INSERT INTO web_university (id, korean_name, campus, region, image_url, created_at, updated_at)
SELECT id, korean_name, campus, region, image_url, created_at, updated_at
FROM university;

-- 웹용 club 테이블 생성 (앱용 club 테이블 구조 복사)
CREATE TABLE IF NOT EXISTS web_club
(
    id                       INT AUTO_INCREMENT PRIMARY KEY,
    university_id            INT                                                            NOT NULL,
    club_category            VARCHAR(255)                                                   NOT NULL,
    name                     VARCHAR(50)                                                    NOT NULL,
    topic                    VARCHAR(20)                                                    NOT NULL DEFAULT '기타',
    description              VARCHAR(30)                                                    NOT NULL,
    introduce                TEXT                                                           NOT NULL,
    image_url                VARCHAR(255)                                                   NOT NULL,
    location                 VARCHAR(255)                                                   NOT NULL,
    fee_amount               VARCHAR(100)                                                   NULL,
    fee_bank                 VARCHAR(100)                                                   NULL,
    fee_account_number       VARCHAR(100)                                                   NULL,
    fee_account_holder       VARCHAR(100)                                                   NULL,
    is_fee_required          TINYINT(1)                                                     DEFAULT 0,
    is_recruitment_enabled   TINYINT(1)                                                     NOT NULL DEFAULT 0,
    is_application_enabled   TINYINT(1)                                                     NOT NULL DEFAULT 0,
    google_sheet_id          VARCHAR(255)                                                   NULL,
    sheet_column_mapping     JSON                                                           NULL,
    drive_folder_id          VARCHAR(255)                                                   NULL,
    template_spreadsheet_id  VARCHAR(255)                                                   NULL,
    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (university_id) REFERENCES web_university (id)
);

-- 앱용 club 데이터를 웹용 테이블로 복사
INSERT INTO web_club (
    id, university_id, club_category, name, topic, description, introduce, image_url, location,
    fee_amount, fee_bank, fee_account_number, fee_account_holder,
    is_fee_required, is_recruitment_enabled, is_application_enabled,
    google_sheet_id, sheet_column_mapping, drive_folder_id, template_spreadsheet_id,
    created_at, updated_at
)
SELECT
    id, university_id, club_category, name, topic, description, introduce, image_url, location,
    fee_amount, fee_bank, fee_account_number, fee_account_holder,
    is_fee_required, is_recruitment_enabled, is_application_enabled,
    google_sheet_id, sheet_column_mapping, drive_folder_id, template_spreadsheet_id,
    created_at, updated_at
FROM club;
