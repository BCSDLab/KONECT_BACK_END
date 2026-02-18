-- 동아리 설정 토글 컬럼 추가 (모집공고 활성화, 지원서 활성화)
ALTER TABLE club ADD COLUMN is_recruitment_enabled TINYINT(1) DEFAULT NULL;
ALTER TABLE club ADD COLUMN is_application_enabled TINYINT(1) DEFAULT NULL;
