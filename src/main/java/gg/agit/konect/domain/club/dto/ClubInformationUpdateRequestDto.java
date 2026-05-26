package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ClubInformationUpdateRequest", description = "동아리 정보 수정 요청")
public record ClubInformationUpdateRequestDto(

    @Schema(description = "동아리 명", example = "BCSD Lab", requiredMode = REQUIRED)
    @NotBlank(message = "동아리 명은 필수입니다.")
    @Size(max = 50, message = "동아리 명은 최대 50자입니다.")
    String clubName,

    @Schema(description = "동아리 분과", example = "ACADEMIC", requiredMode = REQUIRED)
    @NotNull(message = "동아리 분과는 필수입니다.")
    ClubCategory clubCategory,

    @Schema(description = "한 줄 소개", example = "코딩 동아리입니다.", requiredMode = REQUIRED)
    @NotBlank(message = "한 줄 소개는 필수입니다.")
    @Size(max = 25, message = "한 줄 소개는 최대 25자입니다.")
    String shortDescription,

    @Schema(
        description = "동아리 로고 이미지 URL",
        example = "https://example.com/logo.png",
        requiredMode = REQUIRED
    )
    @NotBlank(message = "동아리 로고 이미지 URL은 필수입니다.")
    @Size(max = 255, message = "동아리 로고 이미지 URL은 최대 255자입니다.")
    String imageUrl,

    @Schema(description = "동아리 방 위치", example = "학생회관 101호", requiredMode = REQUIRED)
    @NotBlank(message = "동아리 위치는 필수입니다.")
    @Size(max = 255, message = "동아리 위치는 최대 255자입니다.")
    String location,

    @Schema(description = "동아리 소개", example = "상세한 동아리 소개 내용...", requiredMode = REQUIRED)
    @NotBlank(message = "동아리 소개는 필수입니다.")
    String fullIntroduction
) {
}
