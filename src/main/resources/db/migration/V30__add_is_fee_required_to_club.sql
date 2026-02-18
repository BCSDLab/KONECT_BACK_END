-- club 테이블에 is_fee_required 컬럼 추가
ALTER TABLE club ADD COLUMN is_fee_required TINYINT(1) DEFAULT NULL;
