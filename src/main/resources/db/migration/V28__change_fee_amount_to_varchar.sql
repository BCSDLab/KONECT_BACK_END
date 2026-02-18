-- 회비 금액 컬럼을 VARCHAR(100)으로 변경 (숫자 + 텍스트 입력 가능하도록)
ALTER TABLE club MODIFY COLUMN fee_amount VARCHAR(100);
