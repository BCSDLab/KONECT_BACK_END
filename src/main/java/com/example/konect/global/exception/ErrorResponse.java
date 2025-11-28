package com.example.konect.global.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "표준 에러 응답 포맷")
public record ErrorResponse(
    @Schema(description = "비즈니스 에러 코드")
    String code,

    @Schema(description = "사용자에게 보여줄 에러 메시지")
    String message,

    @Schema(description = "에러 추적용 UUID")
    String errorTraceId,

    @Schema(description = "필드별 검증 오류 목록")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<FieldError> fieldErrors
) {

    public ErrorResponse(String code, String message, String errorTraceId) {
        this(code, message, errorTraceId, List.of());
    }

    @Schema(description = "필드별 검증 오류 목록 아이템")
    public record FieldError(
        @Schema(description = "오류가 발생한 필드 이름 (snake_case)")
        String field,

        @Schema(description = "해당 필드의 오류 메시지")
        String message,

        @Schema(description = "해당 필드의 오류 코드(제약조건 이름 등)")
        String constraint
    ) {
        public FieldError(String field, String message, String constraint) {
            this.field = field;
            this.message = message;
            this.constraint = constraint;
        }
    }
}

