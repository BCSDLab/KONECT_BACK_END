-- 회비 납부 기한 컬럼 삭제
ALTER TABLE club DROP COLUMN fee_deadline;

-- 회비 납부 필요 여부를 club 테이블로 이동
ALTER TABLE club ADD COLUMN is_fee_required TINYINT(1) NULL;

-- 기존 데이터 마이그레이션 (club_recruitment -> club)
UPDATE club c
    INNER JOIN club_recruitment cr ON c.id = cr.club_id
SET c.is_fee_required = cr.is_fee_required;

-- club_recruitment에서 is_fee_required 컬럼 삭제
ALTER TABLE club_recruitment DROP COLUMN is_fee_required;
