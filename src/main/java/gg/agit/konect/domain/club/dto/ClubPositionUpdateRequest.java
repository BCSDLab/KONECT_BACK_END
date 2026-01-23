package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubPositionUpdateRequest(
    @NotBlank(message = "직책 이름은 필수 입력입니다.")
    @Size(max = 50, message = "직책 이름은 최대 50자까지 입력 가능합니다.")
    @Schema(description = "직책 이름", example = "총무", requiredMode = REQUIRED)
    String name
) {

}
