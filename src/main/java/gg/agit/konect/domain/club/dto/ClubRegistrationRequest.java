package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubCategory;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubRegistrationRequest(
    @NotBlank(message = "대학교 명은 필수 입력입니다.")
    @Size(max = 50, message = "대학교 명은 50자 이하여야 합니다.")
    @Schema(description = "대학교 명", example = "한국기술교육대학교", requiredMode = REQUIRED)
    String universityName,

    @NotBlank(message = "동아리 명은 필수 입력입니다.")
    @Size(max = 50, message = "동아리 명은 50자 이하여야 합니다.")
    @Schema(description = "동아리 명", example = "BCSD Lab", requiredMode = REQUIRED)
    String clubName,

    @NotNull(message = "동아리 분과는 필수 입력입니다.")
    @Schema(description = "동아리 분과", example = "ACADEMIC", requiredMode = REQUIRED)
    ClubCategory clubCategory,

    @NotBlank(message = "동아리 주제는 필수 입력입니다.")
    @Size(max = 20, message = "동아리 주제는 20자 이하여야 합니다.")
    @Schema(description = "동아리 주제", example = "개발", requiredMode = REQUIRED)
    String topic,

    @NotBlank(message = "동아리 이모지는 필수 입력입니다.")
    @Size(max = 20, message = "동아리 이모지는 20자 이하여야 합니다.")
    @Schema(description = "동아리 텍스트 이모지", example = "💻", requiredMode = REQUIRED)
    String emoji,

    @NotBlank(message = "한 줄 소개는 필수 입력입니다.")
    @Size(max = 30, message = "한 줄 소개는 30자 이하여야 합니다.")
    @Schema(description = "한 줄 소개", example = "즐겁게 서비스 만드는 동아리", requiredMode = REQUIRED)
    String description,

    @NotEmpty(message = "사진 및 영상은 필수 입력입니다.")
    @Size(max = 5, message = "사진 및 영상은 5개 이하여야 합니다.")
    @ArraySchema(
        schema = @Schema(description = "사진 및 영상 URL", example = "https://example.com/club-1.png"),
        arraySchema = @Schema(description = "사진 및 영상 URL 목록", requiredMode = REQUIRED)
    )
    List<@NotBlank(message = "사진 및 영상 URL은 비어 있을 수 없습니다.")
    @Size(max = 255, message = "사진 및 영상 URL은 255자 이하여야 합니다.") String> mediaUrls,

    @NotBlank(message = "동아리 소개는 필수 입력입니다.")
    @Size(max = 2000, message = "동아리 소개는 2000자 이하여야 합니다.")
    @Schema(description = "동아리 소개", example = "BCSD Lab은 IT 서비스 개발 동아리입니다.", requiredMode = REQUIRED)
    String introduce
) {
}
