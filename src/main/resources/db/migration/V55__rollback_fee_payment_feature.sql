-- V51 rollback: drop club_fee_payment table
DROP TABLE IF EXISTS club_fee_payment;

-- V53 rollback: drop fee_sheet columns from club
ALTER TABLE club
    DROP COLUMN fee_sheet_id;

ALTER TABLE club
    DROP COLUMN fee_sheet_column_mapping;
