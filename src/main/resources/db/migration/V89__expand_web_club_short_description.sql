ALTER TABLE web_club
    MODIFY COLUMN description VARCHAR(100) NOT NULL;

ALTER TABLE club_registration_request
    MODIFY COLUMN short_description VARCHAR(100) NOT NULL;

ALTER TABLE club_information_update_request
    MODIFY COLUMN short_description VARCHAR(100) NOT NULL;
