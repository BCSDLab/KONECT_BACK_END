package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import java.time.LocalDate;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ClubFeeInfoReplaceRequest(
    @PositiveOrZero(message = "회비 금액은 0 이상이어야 합니다.")
    @Schema(description = "회비 금액", example = "10000", requiredMode = NOT_REQUIRED)
    Integer amount,

    @Schema(description = "은행 고유 ID", example = "1", requiredMode = NOT_REQUIRED)
    Integer bankId,

    @Size(max = 100, message = "계좌번호는 최대 100자 입니다.")
    @Schema(description = "계좌번호", example = "123-456-7890", requiredMode = NOT_REQUIRED)
    String accountNumber,

    @Size(max = 100, message = "예금주는 최대 100자 입니다.")
    @Schema(description = "예금주", example = "BCSD", requiredMode = NOT_REQUIRED)
    String accountHolder,

    @Schema(description = "납부 기한", example = "2025.12.31", requiredMode = NOT_REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd")
    LocalDate deadLine
) {
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "회비 정보를 삭제하려면 모든 필드를 비우거나, 모두 입력해야 합니다.")
    public boolean isReplaceRequestValid() {
        boolean allNull = amount == null
            && bankId == null
            && accountNumber == null
            && accountHolder == null
            && deadLine == null;

        if (allNull) {
            return true;
        }

        return amount != null
            && bankId != null
            && StringUtils.hasText(accountNumber)
            && StringUtils.hasText(accountHolder)
            && deadLine != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isDeleteRequest() {
        return amount == null
            && bankId == null
            && accountNumber == null
            && accountHolder == null
            && deadLine == null;
    }
}
