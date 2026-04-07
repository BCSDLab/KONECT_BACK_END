package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubPosition;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SheetImportConfirmRequest(
    @NotNull(message = "최종 등록할 부원 목록은 필수입니다.")
    @Valid
    @ArraySchema(schema = @Schema(implementation = ConfirmMember.class))
    List<ConfirmMember> members
) {
    public record ConfirmMember(
        @NotEmpty(message = "학번은 필수 입력입니다.")
        @Size(min = 4, max = 20, message = "학번은 4자 이상 20자 이하입니다.")
        @Pattern(regexp = "^[0-9]+$", message = "학번은 숫자만 입력할 수 있습니다.")
        @Schema(description = "학번", example = "2021136089", requiredMode = REQUIRED)
        String studentNumber,

        @NotEmpty(message = "이름은 필수 입력입니다.")
        @Pattern(
            regexp = "^([가-힣]{2,5}|(?=.{2,30}$)[A-Za-z]+( [A-Za-z]+)*)$",
            message = "이름은 완성된 한글 2~5자 또는 영문 2~30자(공백 포함)만 입력할 수 있습니다."
        )
        @Schema(description = "이름", example = "김코넥트", requiredMode = REQUIRED)
        String name,

        @Schema(description = "등록할 동아리 직책", example = "MEMBER", requiredMode = NOT_REQUIRED)
        ClubPosition clubPosition,

        @Schema(description = "최종 등록 대상 여부", example = "true", requiredMode = NOT_REQUIRED)
        Boolean enabled
    ) {
        public boolean isEnabled() {
            return !Boolean.FALSE.equals(enabled);
        }
    }
}
