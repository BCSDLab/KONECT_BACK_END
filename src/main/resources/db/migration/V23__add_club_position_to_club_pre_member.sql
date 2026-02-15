ALTER TABLE club_pre_member
    ADD COLUMN club_position VARCHAR(20) NOT NULL DEFAULT 'MEMBER' AFTER name;
