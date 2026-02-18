package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.club.model.Club;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubFeeInfoResponse(
    @Schema(description = "회비 금액", example = "3만원", requiredMode = REQUIRED)
    String amount,

    @Schema(description = "은행 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer bankId,

    @Schema(description = "은행명", example = "국민은행", requiredMode = REQUIRED)
    String bankName,

    @Schema(description = "계좌번호", example = "123-456-7890", requiredMode = REQUIRED)
    String accountNumber,

    @Schema(description = "예금주", example = "BCSD", requiredMode = REQUIRED)
    String accountHolder,

    @Schema(description = "납부 기한", example = "2025.12.31", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd")
    LocalDate deadLine
) {
    public static ClubFeeInfoResponse of(Club club, Integer bankId, String bankName) {
        return new ClubFeeInfoResponse(
            club.getFeeAmount(),
            bankId,
            bankName,
            club.getFeeAccountNumber(),
            club.getFeeAccountHolder(),
            club.getFeeDeadline()
        );
    }
}
