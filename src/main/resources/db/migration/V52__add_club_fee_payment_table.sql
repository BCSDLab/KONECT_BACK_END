CREATE TABLE club_fee_payment (
    id                INT          NOT NULL AUTO_INCREMENT,
    club_id           INT          NOT NULL,
    user_id           INT          NOT NULL,
    is_paid           TINYINT(1)   NOT NULL DEFAULT 0,
    payment_image_url VARCHAR(255),
    approved_at       TIMESTAMP    NULL,
    approved_by       INT          NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_fee_payment_club         FOREIGN KEY (club_id)     REFERENCES club (id),
    CONSTRAINT fk_fee_payment_user         FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_fee_payment_approved_by  FOREIGN KEY (approved_by) REFERENCES users (id),
    UNIQUE KEY uq_fee_payment_club_user (club_id, user_id)
);
