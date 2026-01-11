CREATE TABLE IF NOT EXISTS bank
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)                                                   NOT NULL,
    image_url  VARCHAR(255)                                                   NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                            NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_bank_name UNIQUE (name)
);

INSERT INTO bank (name, image_url)
VALUES ('국민은행', 'https://example.com/banks/kookmin.png'),
       ('신한은행', 'https://example.com/banks/shinhan.png'),
       ('우리은행', 'https://example.com/banks/woori.png'),
       ('하나은행', 'https://example.com/banks/hana.png'),
       ('농협은행', 'https://example.com/banks/nh.png'),
       ('기업은행', 'https://example.com/banks/ibk.png'),
       ('산업은행', 'https://example.com/banks/kdb.png'),
       ('SC제일은행', 'https://example.com/banks/sc.png'),
       ('씨티은행', 'https://example.com/banks/citi.png'),
       ('카카오뱅크', 'https://example.com/banks/kakao.png'),
       ('케이뱅크', 'https://example.com/banks/kbank.png'),
       ('토스뱅크', 'https://example.com/banks/toss.png'),
       ('우체국', 'https://example.com/banks/epost.png'),
       ('새마을금고', 'https://example.com/banks/mg.png'),
       ('신협', 'https://example.com/banks/cu.png'),
       ('수협', 'https://example.com/banks/suhyup.png'),
       ('부산은행', 'https://example.com/banks/bnk.png'),
       ('대구은행', 'https://example.com/banks/dgb.png'),
       ('광주은행', 'https://example.com/banks/kj.png'),
       ('전북은행', 'https://example.com/banks/jb.png'),
       ('경남은행', 'https://example.com/banks/kn.png'),
       ('제주은행', 'https://example.com/banks/jj.png');
