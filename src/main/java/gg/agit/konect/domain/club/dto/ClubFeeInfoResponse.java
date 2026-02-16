package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.model.Club;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubFeeInfoResponse(
    @Schema(description = "회비 금액", example = "10000", requiredMode = REQUIRED)
    Integer amount,

    @Schema(description = "은행명", example = "국민은행", requiredMode = REQUIRED)
    String bank,

    @Schema(description = "계좌번호", example = "123-456-7890", requiredMode = REQUIRED)
    String accountNumber,

    @Schema(description = "예금주", example = "BCSD", requiredMode = REQUIRED)
    String accountHolder,

    @Schema(description = "회비 납부 필요 여부", example = "true", requiredMode = REQUIRED)
    Boolean isFeeRequired
) {
    /**
     * Create a ClubFeeInfoResponse containing fee information extracted from the given Club.
     *
     * @param club the source Club from which fee fields are read
     * @return a ClubFeeInfoResponse populated with the club's fee amount, bank, account number, account holder, and fee-required flag
     */
    public static ClubFeeInfoResponse from(Club club) {
        return new ClubFeeInfoResponse(
            club.getFeeAmount(),
            club.getFeeBank(),
            club.getFeeAccountNumber(),
            club.getFeeAccountHolder(),
            club.getIsFeeRequired()
        );
    }
}