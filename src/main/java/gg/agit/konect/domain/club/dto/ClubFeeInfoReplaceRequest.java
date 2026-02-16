package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ClubFeeInfoReplaceRequest(
    @PositiveOrZero(message = "회비 금액은 0 이상이어야 합니다.")
    @Schema(description = "회비 금액", example = "10000", requiredMode = NOT_REQUIRED)
    Integer amount,

    @NotNull(message = "은행은 필수로 입력해야 합니다.")
    @Schema(description = "은행 고유 ID", example = "1", requiredMode = NOT_REQUIRED)
    Integer bankId,

    @Size(max = 100, message = "계좌번호는 최대 100자 입니다.")
    @Schema(description = "계좌번호", example = "123-456-7890", requiredMode = NOT_REQUIRED)
    String accountNumber,

    @Size(max = 100, message = "예금주는 최대 100자 입니다.")
    @Schema(description = "예금주", example = "BCSD", requiredMode = NOT_REQUIRED)
    String accountHolder,

    @NotNull(message = "회비 납부 필요 여부는 필수로 입력해야 합니다.")
    @Schema(description = "회비 납부 필요 여부", example = "true", requiredMode = REQUIRED)
    Boolean isFeeRequired
) {
}
