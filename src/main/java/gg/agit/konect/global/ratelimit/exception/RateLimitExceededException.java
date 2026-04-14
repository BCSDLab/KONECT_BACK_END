package gg.agit.konect.global.ratelimit.exception;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

/**
 * Rate Limit 초과 시 발생하는 예외 팩토리.
 * CustomException은 상속이 불가능하므로 팩토리 메서드로 생성합니다.
 */
public final class RateLimitExceededException {

    private static final String MESSAGE_TEMPLATE = "요청 횟수가 너무 많습니다. %d초 후 다시 시도해주세요.";

    private RateLimitExceededException() {
        // 유틸리티 클래스
    }

    /**
     * 남은 시간(초)을 포함하여 예외를 생성합니다.
     *
     * @param remainingSeconds 남은 시간(초)
     * @return CustomException
     */
    public static CustomException withRemainingTime(long remainingSeconds) {
        return CustomException.of(
            ApiResponseCode.TOO_MANY_REQUESTS,
            String.format(MESSAGE_TEMPLATE, remainingSeconds)
        );
    }

}
