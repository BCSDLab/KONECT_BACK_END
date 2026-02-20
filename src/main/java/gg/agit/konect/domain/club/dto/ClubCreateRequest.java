package gg.agit.konect.domain.club.dto;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.model.University;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubCreateRequest(
    @Schema(description = "동아리 이름", example = "BCSD Lab", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "동아리 이름은 필수 입력입니다.")
    @Size(max = 50, message = "동아리 이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "동아리 한 줄 소개", example = "즐겁게 일하고 노는 IT 동아리",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "동아리 소개는 필수 입력입니다.")
    @Size(max = 25, message = "동아리 소개는 25자 이하여야 합니다.")
    String description,

    @Schema(description = "동아리 상세 소개", example = "BCSD에서 얻을 수 있는 경험\n1. IT 실무 경험",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "상세 소개는 필수 입력입니다.")
    String introduce,

    @Schema(description = "동아리 로고 이미지 URL", example = "https://example.com/logo.png",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이미지 URL은 필수 입력입니다.")
    @Size(max = 255, message = "이미지 URL은 255자 이하여야 합니다.")
    String imageUrl,

    @Schema(description = "동아리 방 위치", example = "학생회관 101호", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "동아리 위치는 필수입니다.")
    @Size(max = 255, message = "동아리 위치는 255자 이하여야 합니다.")
    String location,

    @Schema(description = "동아리 분과", example = "ACADEMIC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "동아리 분과는 필수입니다.")
    ClubCategory clubCategory
) {
    public Club toEntity(University university) {
        return Club.builder()
            .name(name)
            .description(description)
            .introduce(introduce)
            .imageUrl(imageUrl)
            .location(location)
            .clubCategory(clubCategory)
            .university(university)
            .build();
    }
}
