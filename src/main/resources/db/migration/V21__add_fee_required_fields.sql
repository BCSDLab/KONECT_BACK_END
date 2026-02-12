-- ClubRecruitment에 회비 납부 필요 여부 필드 추가
ALTER TABLE club_recruitment
    ADD COLUMN is_fee_required TINYINT(1) DEFAULT 0 NOT NULL AFTER is_always_recruiting;

-- ClubApply에 회비 납부 증빙 사진 URL 필드 추가
ALTER TABLE club_apply
    ADD COLUMN fee_payment_image_url VARCHAR(512) NULL AFTER user_id;
