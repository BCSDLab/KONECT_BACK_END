package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ClubRegistrationRequest", description = "동아리 등록 요청")
public record ClubRegistrationRequestDto(

    @Schema(description = "대학교 명", example = "한국기술교육대학교", requiredMode = REQUIRED)
    @NotBlank(message = "대학교 명은 필수입니다.")
    String universityName,

    @Schema(description = "동아리 명", example = "BCSD Lab", requiredMode = REQUIRED)
    @NotBlank(message = "동아리 명은 필수입니다.")
    @Size(max = 50, message = "동아리 명은 최대 50자입니다.")
    String clubName,

    @Schema(description = "동아리 분과", example = "ACADEMIC", requiredMode = REQUIRED)
    @NotNull(message = "동아리 분과는 필수입니다.")
    ClubCategory clubCategory,

    @Schema(description = "동아리 주제", example = "코딩", requiredMode = REQUIRED)
    @NotBlank(message = "동아리 주제는 필수입니다.")
    @Size(max = 20, message = "동아리 주제는 최대 20자입니다.")
    String clubTopic,

    @Schema(description = "동아리 이모지", example = "💻", requiredMode = REQUIRED)
    @NotBlank(message = "동아리 이모지는 필수입니다.")
    @Size(max = 10, message = "동아리 이모지는 최대 10자입니다.")
    String clubEmoji,

    @Schema(description = "한 줄 소개 (최대 30자)", example = "코딩 동아리입니다.", requiredMode = REQUIRED)
    @NotBlank(message = "한 줄 소개는 필수입니다.")
    @Size(max = 30, message = "한 줄 소개는 최대 30자입니다.")
    String shortDescription,

    @Schema(description = "동아리 소개 (최대 2000자)", example = "상세한 동아리 소개 내용...", requiredMode = REQUIRED)
    @NotBlank(message = "동아리 소개는 필수입니다.")
    @Size(max = 2000, message = "동아리 소개는 최대 2000자입니다.")
    String fullIntroduction,

    @Schema(
        description = "사진 및 영상 URL 목록 (최대 5개)",
        example = "[\"https://example.com/image1.jpg\"]",
        requiredMode = NOT_REQUIRED
    )
    @Size(max = 5, message = "사진 및 영상은 최대 5개까지 업로드 가능합니다.")
    List<String> imageUrls
) {
}
