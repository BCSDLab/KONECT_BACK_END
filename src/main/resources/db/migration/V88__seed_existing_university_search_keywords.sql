-- V88 uses expected_keyword JOIN university to seed only universities that already exist.
-- ON DUPLICATE KEY UPDATE keeps this corrective seed safe when keywords were partially inserted.
INSERT INTO university_search_keyword (university_id, keyword, normalized_keyword, keyword_type)
SELECT university.id, expected_keyword.keyword, expected_keyword.normalized_keyword, expected_keyword.keyword_type
FROM (
    SELECT '가톨릭대학교' AS university_name, '가대' AS keyword, '가대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '건국대학교' AS university_name, '건대' AS keyword, '건대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '경북대학교' AS university_name, '경대' AS keyword, '경대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '경북대학교' AS university_name, '경북대' AS keyword, '경북대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '경희대학교' AS university_name, '경희대' AS keyword, '경희대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '고려대학교' AS university_name, '고대' AS keyword, '고대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '광주과학기술원' AS university_name, '광주과기원' AS keyword, '광주과기원' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '광주과학기술원' AS university_name, '지스트' AS keyword, '지스트' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '광주과학기술원' AS university_name, 'gist' AS keyword, 'gist' AS normalized_keyword, 'ENGLISH_ALIAS' AS keyword_type
    UNION ALL SELECT '단국대학교' AS university_name, '단대' AS keyword, '단대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '대구경북과학기술원' AS university_name, '대경과기원' AS keyword, '대경과기원' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '대구경북과학기술원' AS university_name, '디지스트' AS keyword, '디지스트' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '대구경북과학기술원' AS university_name, 'dgist' AS keyword, 'dgist' AS normalized_keyword, 'ENGLISH_ALIAS' AS keyword_type
    UNION ALL SELECT '동국대학교' AS university_name, '동대' AS keyword, '동대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '부산대학교' AS university_name, '부대' AS keyword, '부대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '부산대학교' AS university_name, '부산대' AS keyword, '부산대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '서강대학교' AS university_name, '서강대' AS keyword, '서강대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '서울과학기술대학교' AS university_name, '과기대' AS keyword, '과기대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '서울과학기술대학교' AS university_name, '서울과기대' AS keyword, '서울과기대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '서울대학교' AS university_name, '설대' AS keyword, '설대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '서울대학교' AS university_name, '서울대' AS keyword, '서울대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '서울시립대학교' AS university_name, '시립대' AS keyword, '시립대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '서울시립대학교' AS university_name, '서울시립대' AS keyword, '서울시립대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '성균관대학교' AS university_name, '성대' AS keyword, '성대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '연세대학교' AS university_name, '연대' AS keyword, '연대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '울산과학기술원' AS university_name, '울산과기원' AS keyword, '울산과기원' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '울산과학기술원' AS university_name, '유니스트' AS keyword, '유니스트' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '울산과학기술원' AS university_name, 'unist' AS keyword, 'unist' AS normalized_keyword, 'ENGLISH_ALIAS' AS keyword_type
    UNION ALL SELECT '육군사관학교' AS university_name, '육사' AS keyword, '육사' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '이화여자대학교' AS university_name, '이대' AS keyword, '이대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '이화여자대학교' AS university_name, '이화여대' AS keyword, '이화여대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '전남대학교' AS university_name, '전대' AS keyword, '전대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '전남대학교' AS university_name, '전남대' AS keyword, '전남대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '중앙대학교' AS university_name, '중대' AS keyword, '중대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '충남대학교' AS university_name, '충대' AS keyword, '충대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '충남대학교' AS university_name, '충남대' AS keyword, '충남대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '충북대학교' AS university_name, '충북대' AS keyword, '충북대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '포항공과대학교' AS university_name, '포공' AS keyword, '포공' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '포항공과대학교' AS university_name, '포스텍' AS keyword, '포스텍' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '포항공과대학교' AS university_name, 'postech' AS keyword, 'postech' AS normalized_keyword, 'ENGLISH_ALIAS' AS keyword_type
    UNION ALL SELECT '한국공학대학교' AS university_name, '한공대' AS keyword, '한공대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국공학대학교' AS university_name, '한국공대' AS keyword, '한국공대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국과학기술원' AS university_name, '카이스트' AS keyword, '카이스트' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국과학기술원' AS university_name, 'kaist' AS keyword, 'kaist' AS normalized_keyword, 'ENGLISH_ALIAS' AS keyword_type
    UNION ALL SELECT '한국교통대학교' AS university_name, '교통대' AS keyword, '교통대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국교통대학교' AS university_name, '한국교통대' AS keyword, '한국교통대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국기술교육대학교' AS university_name, '한기대' AS keyword, '한기대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국기술교육대학교' AS university_name, '코리아텍' AS keyword, '코리아텍' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국기술교육대학교' AS university_name, 'koreatech' AS keyword, 'koreatech' AS normalized_keyword, 'ENGLISH_ALIAS' AS keyword_type
    UNION ALL SELECT '한국외국어대학교' AS university_name, '외대' AS keyword, '외대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국외국어대학교' AS university_name, '한국외대' AS keyword, '한국외대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국체육대학교' AS university_name, '한체대' AS keyword, '한체대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국항공대학교' AS university_name, '항공대' AS keyword, '항공대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국항공대학교' AS university_name, '한국항공대' AS keyword, '한국항공대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국해양대학교' AS university_name, '해양대' AS keyword, '해양대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '한국해양대학교' AS university_name, '한국해양대' AS keyword, '한국해양대' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '해군사관학교' AS university_name, '해사' AS keyword, '해사' AS normalized_keyword, 'ALIAS' AS keyword_type
    UNION ALL SELECT '홍익대학교' AS university_name, '홍대' AS keyword, '홍대' AS normalized_keyword, 'ALIAS' AS keyword_type
) expected_keyword
JOIN university ON university.korean_name = expected_keyword.university_name
ON DUPLICATE KEY UPDATE
    keyword = VALUES(keyword),
    keyword_type = VALUES(keyword_type);
