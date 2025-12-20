package gg.agit.konect.global.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
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

    // 401 Unauthorized
    INVALID_SESSION(HttpStatus.UNAUTHORIZED, "올바르지 않은 인증 정보 입니다."),

    // 403 Forbidden (접근 권한 없음)
    FORBIDDEN_CHAT_ROOM_ACCESS(HttpStatus.FORBIDDEN, "채팅방에 접근할 권한이 없습니다."),

    // 404 Not Found (리소스를 찾을 수 없음)
    NO_HANDLER_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 API 경로입니다."),
    NOT_FOUND_CLUB(HttpStatus.NOT_FOUND, "동아리를 찾을 수 업습니다."),
    NOT_FOUND_CLUB_MEMBER(HttpStatus.NOT_FOUND, "해당하는 동아리 원을 찾을 수 없습니다."),
    NOT_FOUND_CLUB_PRESIDENT(HttpStatus.NOT_FOUND, "동아리 회장을 찾을 수 없습니다."),
    NOT_FOUND_COUNCIL(HttpStatus.NOT_FOUND, "총동아리연합회를 찾을 수 없습니다."),
    NOT_FOUND_COUNCIL_NOTICE(HttpStatus.NOT_FOUND, "총동아리연합회 공지사항을 찾을 수 없습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    NOT_FOUND_UNREGISTERED_USER(HttpStatus.NOT_FOUND, "임시 유저를 찾을 수 없습니다."),
    UNIVERSITY_NOT_FOUND(HttpStatus.NOT_FOUND, "대학교를 찾을 수 없습니다."),

    // 405 Method Not Allowed (지원하지 않는 HTTP 메소드)
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메소드 입니다."),

    // 409 CONFLICT (중복 혹은 충돌)
    OPTIMISTIC_LOCKING_FAILURE(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, "이미 가입된 회원입니다."),
    DUPLICATE_STUDENT_NUMBER(HttpStatus.CONFLICT, "이미 사용 중인 학번입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다."),

    // 500 Internal Server Error (서버 오류)
    CLIENT_ABORTED(HttpStatus.INTERNAL_SERVER_ERROR, "클라이언트에 의해 연결이 중단되었습니다."),
    UNEXPECTED_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 예기치 못한 에러가 발생했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
