CREATE TABLE university_search_keyword
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    university_id      INT                                                            NOT NULL,
    keyword            VARCHAR(100)                                                   NOT NULL,
    normalized_keyword VARCHAR(100)                                                   NOT NULL,
    keyword_type       VARCHAR(50)                                                    NOT NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_university_search_keyword_keyword_type CHECK (keyword_type IN ('ALIAS', 'ENGLISH_ALIAS')),
    CONSTRAINT fk_university_search_keyword_university FOREIGN KEY (university_id) REFERENCES university (id),
    CONSTRAINT uq_university_search_keyword_university_keyword UNIQUE (university_id, normalized_keyword)
);

CREATE INDEX idx_university_search_keyword_normalized_keyword
    ON university_search_keyword (normalized_keyword);
