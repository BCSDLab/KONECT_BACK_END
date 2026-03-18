-- V51 rollback: drop club_fee_payment table
DROP TABLE IF EXISTS club_fee_payment;

-- V53 rollback: drop fee_sheet columns from club
ALTER TABLE club
    DROP COLUMN IF EXISTS fee_sheet_id,
    DROP COLUMN IF EXISTS fee_sheet_column_mapping;
