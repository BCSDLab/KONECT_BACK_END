-- 카테고리
INSERT INTO club_category (name) VALUES ('학술');
INSERT INTO club_category (name) VALUES ('운동');
INSERT INTO club_category (name) VALUES ('취미');
INSERT INTO club_category (name) VALUES ('종교');
INSERT INTO club_category (name) VALUES ('공연');

-- 동아리
INSERT INTO club (club_category_id, name, description, image_url)
VALUES (1, 'BCSD', '즐겁게 일하고 열심히 노는 IT 특성화 동아리! 코인 만든 동아리예요~',
        'https://static.koreatech.in/upload/CLUB/2025/6/10/d0320625-7055-4a33-aad7-ee852a008ce7/BCSD Logo-symbol.png');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (2, 'CUT', '한기대 탁구동아리🏓',
        'https://static.koreatech.in/upload/LOST_ITEMS/2025/6/12/bbacbbb4-5f64-4582-8f5f-e6e446031362/1000035027.jpg');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (5, 'K-오케스트라', '아름다운 음악과 재미있는 합주!',
        'https://static.koreatech.in/upload/LOST_ITEMS/2025/6/15/e12716ab-d5bc-4143-9101-5a2b6f0bfb94/1000014263.jpg');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (2, '스텝업', '클라이밍 붐은 온다. 🧗',
        'https://static.koreatech.in/upload/CLUB/2025/7/7/a72b37fb-e82b-4cbb-a2c2-4c59d8fc6b84/923ECFF9-871B-40A3-A13A-8230F0B666F8.jpeg');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (1, 'K-ROAD', 'K-ROAD는 자율주행 연구와 대회 참가로 성과를 내는연구 단체입니다.',
        'https://static.koreatech.in/upload/CLUB/2025/7/8/7b60e632-0d24-4200-9891-a6dc15a72330/IMG_6794.png');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (2, 'S.A.M', '안녕하세요! 스쿼시 동아리 S.A.M 입니다!',
        'https://static.koreatech.in/upload/CLUB/2025/7/17/446479f5-c109-48c5-bd63-f38942ef356d/sam 로고 반전.jpg');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (2, 'SMASH', '한국기술교육대학교 테니스 동아리',
        'https://static.koreatech.in/upload/CLUB/2025/7/30/611ddcef-faa9-4302-8112-092ea3a48e67/1000031983.jpg');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (4, 'SED-TUA', '한기대 가톨릭/천주교 동아리',
        'https://static.koreatech.in/upload/CLUB/2025/8/6/34c9902e-fbc7-4e5e-96f2-8e53bfe601b4/1000003833.jpg');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (5, '비상', '낭만과 행복이 가득한 어쿠스틱 기타 공연 동아리',
        'https://static.koreatech.in/upload/CLUB/2025/8/8/84a933e3-4473-45e3-9057-cc573acdb982/1000034369.png');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (3, '셔터', '안녕하세요! 한국기술대학교 사진 동아리 ''셔터''입니다!',
        'https://static.koreatech.in/upload/CLUB/2025/8/16/429d1be5-da62-4f53-b033-f6e01a55feeb/5763.png');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (5, '극예술연구회', '행동하는 젊음! 연극을 만들고 낭만을 새기는 동아리, 극예술연구회입니다.',
        'https://static.koreatech.in/upload/CLUB/2025/9/3/f3821e52-c695-4482-8114-862e8bde4527/9498.png');

INSERT INTO club (club_category_id, name, description, image_url)
VALUES (5, '한소리', '한기대 유일무이 풍물패 입니다!',
        'https://static.koreatech.in/upload/CLUB/2025/10/13/c23361f0-f7ff-4eee-ae9c-7577d5bab4da/1000006554.jpg');

-- 태그
INSERT INTO club_tag (name) VALUES ('IT');
INSERT INTO club_tag (name) VALUES ('프로그래밍');
INSERT INTO club_tag (name) VALUES ('스터디');
INSERT INTO club_tag (name) VALUES ('프로젝트');
INSERT INTO club_tag (name) VALUES ('탁구');
INSERT INTO club_tag (name) VALUES ('운동');
INSERT INTO club_tag (name) VALUES ('음악');
INSERT INTO club_tag (name) VALUES ('합주');
INSERT INTO club_tag (name) VALUES ('공연');
INSERT INTO club_tag (name) VALUES ('클라이밍');
INSERT INTO club_tag (name) VALUES ('자율주행');
INSERT INTO club_tag (name) VALUES ('연구');
INSERT INTO club_tag (name) VALUES ('대회');
INSERT INTO club_tag (name) VALUES ('스쿼시');
INSERT INTO club_tag (name) VALUES ('테니스');
INSERT INTO club_tag (name) VALUES ('가톨릭');
INSERT INTO club_tag (name) VALUES ('종교');
INSERT INTO club_tag (name) VALUES ('봉사');
INSERT INTO club_tag (name) VALUES ('기타');
INSERT INTO club_tag (name) VALUES ('사진');
INSERT INTO club_tag (name) VALUES ('촬영');
INSERT INTO club_tag (name) VALUES ('연극');
INSERT INTO club_tag (name) VALUES ('예술');
INSERT INTO club_tag (name) VALUES ('풍물');
INSERT INTO club_tag (name) VALUES ('전통');

-- 동아리 태그 매핑
-- BCSD (club_id = 1)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (1, 1);  -- IT
INSERT INTO club_tag_map (club_id, tag_id) VALUES (1, 2);  -- 프로그래밍
INSERT INTO club_tag_map (club_id, tag_id) VALUES (1, 3);  -- 스터디
INSERT INTO club_tag_map (club_id, tag_id) VALUES (1, 4);  -- 프로젝트

-- CUT (club_id = 2)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (2, 5);  -- 탁구
INSERT INTO club_tag_map (club_id, tag_id) VALUES (2, 6);  -- 운동

-- K-오케스트라 (club_id = 3)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (3, 7);  -- 음악
INSERT INTO club_tag_map (club_id, tag_id) VALUES (3, 8);  -- 합주
INSERT INTO club_tag_map (club_id, tag_id) VALUES (3, 9);  -- 공연

-- 스텝업 (club_id = 4)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (4, 10); -- 클라이밍
INSERT INTO club_tag_map (club_id, tag_id) VALUES (4, 6);  -- 운동

-- K-ROAD (club_id = 5)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (5, 11); -- 자율주행
INSERT INTO club_tag_map (club_id, tag_id) VALUES (5, 12); -- 연구
INSERT INTO club_tag_map (club_id, tag_id) VALUES (5, 13); -- 대회

-- S.A.M (club_id = 6)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (6, 14); -- 스쿼시
INSERT INTO club_tag_map (club_id, tag_id) VALUES (6, 6);  -- 운동

-- SMASH (club_id = 7)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (7, 15); -- 테니스
INSERT INTO club_tag_map (club_id, tag_id) VALUES (7, 6);  -- 운동

-- SED-TUA (club_id = 8)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (8, 16); -- 가톨릭
INSERT INTO club_tag_map (club_id, tag_id) VALUES (8, 17); -- 종교

-- 비상 (club_id = 9)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (9, 7);  -- 음악
INSERT INTO club_tag_map (club_id, tag_id) VALUES (9, 9);  -- 공연
INSERT INTO club_tag_map (club_id, tag_id) VALUES (9, 19); -- 기타

-- 셔터 (club_id = 10)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (10, 20); -- 사진
INSERT INTO club_tag_map (club_id, tag_id) VALUES (10, 21); -- 촬영

-- 극예술연구회 (club_id = 11)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (11, 22); -- 연극
INSERT INTO club_tag_map (club_id, tag_id) VALUES (11, 23); -- 예술

-- 한소리 (club_id = 12)
INSERT INTO club_tag_map (club_id, tag_id) VALUES (12, 24); -- 풍물
INSERT INTO club_tag_map (club_id, tag_id) VALUES (12, 25); -- 전통

INSERT INTO club_recruitment (club_id, start_date, end_date)
VALUES (1, '2025-11-30', '2025-12-31'),
       (2, '2025-11-29', '2025-12-31'),
       (3, '2025-11-28', '2025-12-31'),
       (4, '2025-11-27', '2025-12-31');
