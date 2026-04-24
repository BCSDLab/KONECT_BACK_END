ALTER TABLE `advertisement`
    COMMENT = '앱 광고 배너와 노출 기간 정보';

ALTER TABLE `bank`
    COMMENT = '회비 납부 등에 사용할 은행 코드와 은행명';

ALTER TABLE `chat_message`
    COMMENT = '채팅방 메시지 내역';

ALTER TABLE `chat_room`
    COMMENT = '1대1, 동아리 단체, 시스템 관리자 채팅방';

ALTER TABLE `chat_room_member`
    COMMENT = '채팅방 참여자, 읽음 시점, 퇴장/숨김 상태';

ALTER TABLE `club`
    COMMENT = '대학교별 동아리 기본 정보와 회비/구글시트 연동 설정';

ALTER TABLE `club_apply`
    COMMENT = '사용자의 동아리 지원 신청';

ALTER TABLE `club_apply_answer`
    COMMENT = '동아리 지원서 질문별 답변';

ALTER TABLE `club_apply_question`
    COMMENT = '동아리 모집 지원서 질문';

ALTER TABLE `club_member`
    COMMENT = '동아리 소속 회원과 역할';

ALTER TABLE `club_pre_member`
    COMMENT = '구글시트에서 가져온 동아리 예비 회원';

ALTER TABLE `club_recruitment`
    COMMENT = '동아리 모집 공고와 모집 기간';

ALTER TABLE `club_recruitment_image`
    COMMENT = '동아리 모집 공고 이미지';

ALTER TABLE `council`
    COMMENT = '대학교 학생회 정보';

ALTER TABLE `council_notice`
    COMMENT = '학생회 공지사항';

ALTER TABLE `council_notice_read_history`
    COMMENT = '학생회 공지사항 사용자별 읽음 기록';

ALTER TABLE `group_chat_message`
    COMMENT = '레거시 그룹 채팅 메시지';

ALTER TABLE `group_chat_read_status`
    COMMENT = '레거시 그룹 채팅 읽음 상태';

ALTER TABLE `group_chat_room`
    COMMENT = '레거시 그룹 채팅방';

ALTER TABLE `notification_device_token`
    COMMENT = '사용자별 푸시 알림 디바이스 토큰';

ALTER TABLE `notification_inbox`
    COMMENT = '사용자 알림함에 저장되는 알림 내역';

ALTER TABLE `notification_mute_setting`
    COMMENT = '사용자별 알림 뮤트 설정';

ALTER TABLE `ranking_type`
    COMMENT = '순공 랭킹 타입, CLUB/STUDENT_NUMBER/PERSONAL';

ALTER TABLE `schedule`
    COMMENT = '서비스 일반 일정';

ALTER TABLE `study_time_daily`
    COMMENT = '사용자별 일별 누적 순공 시간, study_date 기준';

ALTER TABLE `study_time_monthly`
    COMMENT = '사용자별 월별 누적 순공 시간, study_month는 월의 첫날';

ALTER TABLE `study_time_ranking`
    COMMENT = '순공 랭킹 집계, 타입과 대학별 대상의 일간/월간 시간';

ALTER TABLE `study_time_total`
    COMMENT = '사용자별 전체 누적 순공 시간';

ALTER TABLE `study_timer`
    COMMENT = '현재 순공 타이머가 실행 중인 사용자 세션';

ALTER TABLE `university`
    COMMENT = '대학교와 캠퍼스 정보';

ALTER TABLE `university_schedule`
    COMMENT = '대학교 학사 일정';

ALTER TABLE `unregistered_user`
    COMMENT = '가입 절차를 완료하지 않은 OAuth 사용자';

ALTER TABLE `user_oauth_account`
    COMMENT = '사용자별 OAuth 제공자 계정과 리프레시 토큰';

ALTER TABLE `users`
    COMMENT = '서비스 사용자 계정, deleted_at이 NULL이면 활성 사용자';

ALTER TABLE `version`
    COMMENT = '앱 버전 관리 정보';
