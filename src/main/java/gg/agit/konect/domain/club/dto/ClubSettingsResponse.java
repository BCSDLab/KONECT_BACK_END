package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동아리 설정 관리 응답")
public record ClubSettingsResponse(
    @Schema(description = "모집공고 활성화 여부", example = "true", requiredMode = REQUIRED)
    Boolean isRecruitmentEnabled,

    @Schema(description = "지원서 활성화 여부", example = "true", requiredMode = REQUIRED)
    Boolean isApplicationEnabled,

    @Schema(description = "회비 활성화 여부", example = "false", requiredMode = REQUIRED)
    Boolean isFeeEnabled,

    @Schema(description = "모집공고 요약 정보", requiredMode = NOT_REQUIRED)
    RecruitmentSummary recruitment,

    @Schema(description = "지원서 요약 정보", requiredMode = NOT_REQUIRED)
    ApplicationSummary application,

    @Schema(description = "회비 요약 정보", requiredMode = NOT_REQUIRED)
    FeeSummary fee
) {
    @Schema(description = "모집공고 요약")
    public record RecruitmentSummary(
        @Schema(description = "모집 시작일", example = "2026.02.02", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd")
        LocalDate startDate,

        @Schema(description = "모집 종료일", example = "2027.02.02", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd")
        LocalDate endDate,

        @Schema(description = "상시 모집 여부", example = "false", requiredMode = REQUIRED)
        Boolean isAlwaysRecruiting
    ) {
    }

    @Schema(description = "지원서 요약")
    public record ApplicationSummary(
        @Schema(description = "문항 개수", example = "3", requiredMode = REQUIRED)
        Integer questionCount
    ) {
    }

    @Schema(description = "회비 요약")
    public record FeeSummary(
        @Schema(description = "회비 금액", example = "3만원", requiredMode = NOT_REQUIRED)
        String amount,

        @Schema(description = "은행 고유 ID", example = "1", requiredMode = NOT_REQUIRED)
        Integer bankId,

        @Schema(description = "은행명", example = "국민은행", requiredMode = NOT_REQUIRED)
        String bankName,

        @Schema(description = "계좌번호", example = "123-456-7890", requiredMode = NOT_REQUIRED)
        String accountNumber,

        @Schema(description = "예금주", example = "BCSD", requiredMode = NOT_REQUIRED)
        String accountHolder
    ) {
    }
}
