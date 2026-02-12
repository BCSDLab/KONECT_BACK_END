package gg.agit.konect.global.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ApiResponseCode {

    // 400 Bad Request (잘못된 요청)
    UNSUPPORTED_OPERATION(HttpStatus.BAD_REQUEST, "지원하지 않는 API 입니다."),
    ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 인자가 전달되었습니다."),
    ILLEGAL_STATE(HttpStatus.BAD_REQUEST, "잘못된 상태로 요청이 들어왔습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "잘못된 입력값이 포함되어 있습니다."),
    INVALID_DATE_TIME(HttpStatus.BAD_REQUEST, "잘못된 날짜 형식입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "요청 값의 타입이 올바르지 않습니다."),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "요청 본문의 JSON 형식이 잘못되었습니다."),
    MISSING_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    FAILED_EXTRACT_EMAIL(HttpStatus.BAD_REQUEST, "OAuth 로그인 과정에서 이메일 정보를 가져올 수 없습니다."),
    FAILED_EXTRACT_PROVIDER_ID(HttpStatus.BAD_REQUEST, "OAuth 로그인 과정에서 제공자 식별자를 가져올 수 없습니다."),
    CANNOT_CREATE_CHAT_ROOM_WITH_SELF(HttpStatus.BAD_REQUEST, "자기 자신과는 채팅방을 만들 수 없습니다."),
    INVALID_CHAT_ROOM_CREATE_REQUEST(HttpStatus.BAD_REQUEST, "clubId 또는 targetUserId 중 하나만 전달해야 합니다."),
    CANNOT_CHANGE_OWN_POSITION(HttpStatus.BAD_REQUEST, "자기 자신의 직책은 변경할 수 없습니다."),
    CANNOT_DELETE_CLUB_PRESIDENT(HttpStatus.BAD_REQUEST, "동아리 회장인 경우 회장을 양도하고 탈퇴해야 합니다."),
    CANNOT_DELETE_ESSENTIAL_POSITION(HttpStatus.BAD_REQUEST, "필수 직책은 삭제할 수 없습니다."),
    CANNOT_MANAGE_HIGHER_POSITION(HttpStatus.BAD_REQUEST, "자신보다 높은 직급의 회원은 관리할 수 없습니다."),
    CANNOT_REMOVE_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 강제 탈퇴시킬 수 없습니다."),
    CANNOT_REMOVE_NON_MEMBER(HttpStatus.BAD_REQUEST, "일반회원만 강제 탈퇴할 수 있습니다. 먼저 직책을 변경한 후 탈퇴시켜주세요."),
    INSUFFICIENT_POSITION_COUNT(HttpStatus.BAD_REQUEST, "해당 그룹에 최소 2개의 직책이 있어야 삭제 가능합니다."),
    MANAGER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "운영진은 최대 20명까지 임명 가능합니다."),
    POSITION_IN_USE(HttpStatus.BAD_REQUEST, "해당 직책을 사용 중인 회원이 있어 삭제할 수 없습니다."),
    POSITION_NAME_DUPLICATED(HttpStatus.BAD_REQUEST, "동일한 직책 이름이 이미 존재합니다."),
    REQUIRED_CLUB_APPLY_ANSWER_MISSING(HttpStatus.BAD_REQUEST, "필수 가입 답변이 누락되었습니다."),
    CANNOT_DELETE_USER_WITH_UNPAID_FEE(HttpStatus.BAD_REQUEST, "미납 회비가 있는 경우 탈퇴할 수 없습니다."),
    STUDY_TIMER_NOT_RUNNING(HttpStatus.BAD_REQUEST, "실행 중인 스터디 타이머가 없습니다."),
    STUDY_TIMER_TIME_MISMATCH(HttpStatus.BAD_REQUEST, "스터디 타이머 시간이 유효하지 않습니다."),
    INVALID_RECRUITMENT_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "상시 모집인 경우 모집 시작일과 마감일을 지정할 수 없습니다."),
    INVALID_RECRUITMENT_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "상시 모집이 아닐 경우 모집 시작일과 마감일이 필수입니다."),
    INVALID_RECRUITMENT_PERIOD(HttpStatus.BAD_REQUEST, "모집 시작일은 모집 마감일보다 이전이어야 합니다."),
    INVALID_FILE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 타입입니다."),
    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth Provider 입니다."),
    INVALID_NOTIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "푸시 알림 토큰이 유효하지 않습니다."),

    // 401 Unauthorized
    INVALID_SESSION(HttpStatus.UNAUTHORIZED, "올바르지 않은 인증 정보 입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    MISSING_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "액세스 토큰이 필요합니다."),
    MALFORMED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "액세스 토큰 형식이 올바르지 않습니다."),
    INVALID_ACCESS_TOKEN_SIGNATURE(HttpStatus.UNAUTHORIZED, "액세스 토큰 서명이 올바르지 않습니다."),
    INVALID_ACCESS_TOKEN_ISSUER(HttpStatus.UNAUTHORIZED, "액세스 토큰 발급자가 올바르지 않습니다."),
    INVALID_ACCESS_TOKEN_CLAIMS(HttpStatus.UNAUTHORIZED, "액세스 토큰 정보가 올바르지 않습니다."),
    INVALID_OAUTH_TOKEN(HttpStatus.UNAUTHORIZED, "올바르지 않은 OAuth 토큰 입니다."),
    INVALID_SIGNUP_TOKEN(HttpStatus.UNAUTHORIZED, "회원가입 토큰이 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 올바르지 않습니다."),

    // 403 Forbidden (접근 권한 없음)
    FORBIDDEN_CHAT_ROOM_ACCESS(HttpStatus.FORBIDDEN, "채팅방에 접근할 권한이 없습니다."),
    FORBIDDEN_CLUB_FEE_INFO(HttpStatus.FORBIDDEN, "회비 정보 조회 권한이 없습니다."),
    FORBIDDEN_CLUB_MANAGER_ACCESS(HttpStatus.FORBIDDEN, "동아리 매니저 권한이 없습니다."),
    FORBIDDEN_CLUB_MEMBER_ACCESS(HttpStatus.FORBIDDEN, "동아리 멤버 조회 권한이 없습니다."),
    FORBIDDEN_CLUB_RECRUITMENT_CREATE(HttpStatus.FORBIDDEN, "동아리 모집 공고를 생성할 권한이 없습니다."),
    FORBIDDEN_COUNCIL_NOTICE_ACCESS(HttpStatus.FORBIDDEN, "총동아리연합회 공지사항 조회 권한이 없습니다."),
    FORBIDDEN_MEMBER_POSITION_CHANGE(HttpStatus.FORBIDDEN, "회원 직책 변경 권한이 없습니다."),
    FORBIDDEN_POSITION_NAME_CHANGE(HttpStatus.FORBIDDEN, "해당 직책의 이름은 변경할 수 없습니다."),
    FORBIDDEN_ROLE_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    FORBIDDEN_ORIGIN_ACCESS(HttpStatus.FORBIDDEN, "허용되지 않은 Origin 입니다."),

    // 404 Not Found (리소스를 찾을 수 없음)
    NO_HANDLER_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 API 경로입니다."),
    NOT_FOUND_CHAT_ROOM(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    NOT_FOUND_CLUB(HttpStatus.NOT_FOUND, "동아리를 찾을 수 없습니다."),
    NOT_FOUND_CLUB_MEMBER(HttpStatus.NOT_FOUND, "해당하는 동아리 원을 찾을 수 없습니다."),
    NOT_FOUND_CLUB_PRESIDENT(HttpStatus.NOT_FOUND, "동아리 회장을 찾을 수 없습니다."),
    NOT_FOUND_CLUB_POSITION(HttpStatus.NOT_FOUND, "동아리 직책을 찾을 수 없습니다."),
    NOT_FOUND_CLUB_RECRUITMENT(HttpStatus.NOT_FOUND, "동아리 모집 공고를 찾을 수 없습니다."),
    NOT_FOUND_CLUB_APPLY(HttpStatus.NOT_FOUND, "동아리 지원 내역을 찾을 수 없습니다."),
    NOT_FOUND_COUNCIL(HttpStatus.NOT_FOUND, "총동아리연합회를 찾을 수 없습니다."),
    NOT_FOUND_COUNCIL_NOTICE(HttpStatus.NOT_FOUND, "총동아리연합회 공지사항을 찾을 수 없습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    NOT_FOUND_SCHEDULE(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    NOT_FOUND_UNREGISTERED_USER(HttpStatus.NOT_FOUND, "임시 유저를 찾을 수 없습니다."),
    UNIVERSITY_NOT_FOUND(HttpStatus.NOT_FOUND, "대학교를 찾을 수 없습니다."),
    NOT_FOUND_CLUB_APPLY_QUESTION(HttpStatus.NOT_FOUND, "동아리 가입 문항을 찾을 수 없습니다."),
    NOT_FOUND_RANKING_TYPE(HttpStatus.NOT_FOUND, "순위 타입을 찾을 수 없습니다."),
    NOT_FOUND_BANK(HttpStatus.NOT_FOUND, "해당하는 은행을 찾을 수 없습니다."),
    NOT_FOUND_VERSION(HttpStatus.NOT_FOUND, "버전을 찾을 수 없습니다."),
    // 405 Method Not Allowed (지원하지 않는 HTTP 메소드)
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메소드 입니다."),

    // 409 CONFLICT (중복 혹은 충돌)
    OPTIMISTIC_LOCKING_FAILURE(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, "이미 가입된 회원입니다."),
    ALREADY_EXIST_COUNCIL(HttpStatus.CONFLICT, "해당 대학교에 이미 총동아리연합회가 존재합니다."),
    DUPLICATE_STUDENT_NUMBER(HttpStatus.CONFLICT, "이미 사용 중인 학번입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다."),
    ALREADY_APPLIED_CLUB(HttpStatus.CONFLICT, "이미 동아리에 가입 신청을 완료했습니다."),
    ALREADY_CLUB_MEMBER(HttpStatus.CONFLICT, "이미 동아리 회원입니다."),
    ALREADY_CLUB_PRE_MEMBER(HttpStatus.CONFLICT, "이미 동아리에 사전 등록된 회원입니다."),
    DUPLICATE_CLUB_APPLY_QUESTION(HttpStatus.CONFLICT, "중복된 가입 문항이 포함되어 있습니다."),
    VICE_PRESIDENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "부회장은 이미 존재합니다."),
    ALREADY_RUNNING_STUDY_TIMER(HttpStatus.CONFLICT, "이미 실행 중인 스터디 타이머가 있습니다."),
    ALREADY_EXIST_CLUB_RECRUITMENT(HttpStatus.CONFLICT, "이미 동아리 모집 공고가 존재합니다."),
    DUPLICATE_VERSION(HttpStatus.CONFLICT, "이미 등록된 버전입니다."),
    DUPLICATE_NOTIFICATION_TOKEN(HttpStatus.CONFLICT, "이미 등록된 알림 토큰입니다."),

    // 500 Internal Server Error (서버 오류)
    CLIENT_ABORTED(HttpStatus.INTERNAL_SERVER_ERROR, "클라이언트에 의해 연결이 중단되었습니다."),
    FAILED_UPLOAD_FILE(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FAILED_SEND_NOTIFICATION(HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송에 실패했습니다."),
    UNEXPECTED_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 예기치 못한 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ApiResponseCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getCode() {
        return this.name();
    }
}
