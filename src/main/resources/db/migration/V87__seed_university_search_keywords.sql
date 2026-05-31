INSERT INTO university_search_keyword (university_id, keyword, normalized_keyword, keyword_type)
SELECT id, '가대', '가대', 'ALIAS'
FROM university
WHERE korean_name = '가톨릭대학교'
UNION ALL
SELECT id, '건대', '건대', 'ALIAS'
FROM university
WHERE korean_name = '건국대학교'
UNION ALL
SELECT id, '경대', '경대', 'ALIAS'
FROM university
WHERE korean_name = '경북대학교'
UNION ALL
SELECT id, '경북대', '경북대', 'ALIAS'
FROM university
WHERE korean_name = '경북대학교'
UNION ALL
SELECT id, '경희대', '경희대', 'ALIAS'
FROM university
WHERE korean_name = '경희대학교'
UNION ALL
SELECT id, '고대', '고대', 'ALIAS'
FROM university
WHERE korean_name = '고려대학교'
UNION ALL
SELECT id, '광주과기원', '광주과기원', 'ALIAS'
FROM university
WHERE korean_name = '광주과학기술원'
UNION ALL
SELECT id, '지스트', '지스트', 'ALIAS'
FROM university
WHERE korean_name = '광주과학기술원'
UNION ALL
SELECT id, 'gist', 'gist', 'ENGLISH_ALIAS'
FROM university
WHERE korean_name = '광주과학기술원'
UNION ALL
SELECT id, '단대', '단대', 'ALIAS'
FROM university
WHERE korean_name = '단국대학교'
UNION ALL
SELECT id, '대경과기원', '대경과기원', 'ALIAS'
FROM university
WHERE korean_name = '대구경북과학기술원'
UNION ALL
SELECT id, '디지스트', '디지스트', 'ALIAS'
FROM university
WHERE korean_name = '대구경북과학기술원'
UNION ALL
SELECT id, 'dgist', 'dgist', 'ENGLISH_ALIAS'
FROM university
WHERE korean_name = '대구경북과학기술원'
UNION ALL
SELECT id, '동대', '동대', 'ALIAS'
FROM university
WHERE korean_name = '동국대학교'
UNION ALL
SELECT id, '부대', '부대', 'ALIAS'
FROM university
WHERE korean_name = '부산대학교'
UNION ALL
SELECT id, '부산대', '부산대', 'ALIAS'
FROM university
WHERE korean_name = '부산대학교'
UNION ALL
SELECT id, '서강대', '서강대', 'ALIAS'
FROM university
WHERE korean_name = '서강대학교'
UNION ALL
SELECT id, '과기대', '과기대', 'ALIAS'
FROM university
WHERE korean_name = '서울과학기술대학교'
UNION ALL
SELECT id, '서울과기대', '서울과기대', 'ALIAS'
FROM university
WHERE korean_name = '서울과학기술대학교'
UNION ALL
SELECT id, '설대', '설대', 'ALIAS'
FROM university
WHERE korean_name = '서울대학교'
UNION ALL
SELECT id, '서울대', '서울대', 'ALIAS'
FROM university
WHERE korean_name = '서울대학교'
UNION ALL
SELECT id, '시립대', '시립대', 'ALIAS'
FROM university
WHERE korean_name = '서울시립대학교'
UNION ALL
SELECT id, '서울시립대', '서울시립대', 'ALIAS'
FROM university
WHERE korean_name = '서울시립대학교'
UNION ALL
SELECT id, '성대', '성대', 'ALIAS'
FROM university
WHERE korean_name = '성균관대학교'
UNION ALL
SELECT id, '연대', '연대', 'ALIAS'
FROM university
WHERE korean_name = '연세대학교'
UNION ALL
SELECT id, '울산과기원', '울산과기원', 'ALIAS'
FROM university
WHERE korean_name = '울산과학기술원'
UNION ALL
SELECT id, '유니스트', '유니스트', 'ALIAS'
FROM university
WHERE korean_name = '울산과학기술원'
UNION ALL
SELECT id, 'unist', 'unist', 'ENGLISH_ALIAS'
FROM university
WHERE korean_name = '울산과학기술원'
UNION ALL
SELECT id, '육사', '육사', 'ALIAS'
FROM university
WHERE korean_name = '육군사관학교'
UNION ALL
SELECT id, '이대', '이대', 'ALIAS'
FROM university
WHERE korean_name = '이화여자대학교'
UNION ALL
SELECT id, '이화여대', '이화여대', 'ALIAS'
FROM university
WHERE korean_name = '이화여자대학교'
UNION ALL
SELECT id, '전대', '전대', 'ALIAS'
FROM university
WHERE korean_name = '전남대학교'
UNION ALL
SELECT id, '전남대', '전남대', 'ALIAS'
FROM university
WHERE korean_name = '전남대학교'
UNION ALL
SELECT id, '중대', '중대', 'ALIAS'
FROM university
WHERE korean_name = '중앙대학교'
UNION ALL
SELECT id, '충대', '충대', 'ALIAS'
FROM university
WHERE korean_name = '충남대학교'
UNION ALL
SELECT id, '충남대', '충남대', 'ALIAS'
FROM university
WHERE korean_name = '충남대학교'
UNION ALL
SELECT id, '충북대', '충북대', 'ALIAS'
FROM university
WHERE korean_name = '충북대학교'
UNION ALL
SELECT id, '포공', '포공', 'ALIAS'
FROM university
WHERE korean_name = '포항공과대학교'
UNION ALL
SELECT id, '포스텍', '포스텍', 'ALIAS'
FROM university
WHERE korean_name = '포항공과대학교'
UNION ALL
SELECT id, 'postech', 'postech', 'ENGLISH_ALIAS'
FROM university
WHERE korean_name = '포항공과대학교'
UNION ALL
SELECT id, '한공대', '한공대', 'ALIAS'
FROM university
WHERE korean_name = '한국공학대학교'
UNION ALL
SELECT id, '한국공대', '한국공대', 'ALIAS'
FROM university
WHERE korean_name = '한국공학대학교'
UNION ALL
SELECT id, '카이스트', '카이스트', 'ALIAS'
FROM university
WHERE korean_name = '한국과학기술원'
UNION ALL
SELECT id, 'kaist', 'kaist', 'ENGLISH_ALIAS'
FROM university
WHERE korean_name = '한국과학기술원'
UNION ALL
SELECT id, '교통대', '교통대', 'ALIAS'
FROM university
WHERE korean_name = '한국교통대학교'
UNION ALL
SELECT id, '한국교통대', '한국교통대', 'ALIAS'
FROM university
WHERE korean_name = '한국교통대학교'
UNION ALL
SELECT id, '한기대', '한기대', 'ALIAS'
FROM university
WHERE korean_name = '한국기술교육대학교'
UNION ALL
SELECT id, '코리아텍', '코리아텍', 'ALIAS'
FROM university
WHERE korean_name = '한국기술교육대학교'
UNION ALL
SELECT id, 'koreatech', 'koreatech', 'ENGLISH_ALIAS'
FROM university
WHERE korean_name = '한국기술교육대학교'
UNION ALL
SELECT id, '외대', '외대', 'ALIAS'
FROM university
WHERE korean_name = '한국외국어대학교'
UNION ALL
SELECT id, '한국외대', '한국외대', 'ALIAS'
FROM university
WHERE korean_name = '한국외국어대학교'
UNION ALL
SELECT id, '한체대', '한체대', 'ALIAS'
FROM university
WHERE korean_name = '한국체육대학교'
UNION ALL
SELECT id, '항공대', '항공대', 'ALIAS'
FROM university
WHERE korean_name = '한국항공대학교'
UNION ALL
SELECT id, '한국항공대', '한국항공대', 'ALIAS'
FROM university
WHERE korean_name = '한국항공대학교'
UNION ALL
SELECT id, '해양대', '해양대', 'ALIAS'
FROM university
WHERE korean_name = '한국해양대학교'
UNION ALL
SELECT id, '한국해양대', '한국해양대', 'ALIAS'
FROM university
WHERE korean_name = '한국해양대학교'
UNION ALL
SELECT id, '해사', '해사', 'ALIAS'
FROM university
WHERE korean_name = '해군사관학교'
UNION ALL
SELECT id, '홍대', '홍대', 'ALIAS'
FROM university
WHERE korean_name = '홍익대학교';
