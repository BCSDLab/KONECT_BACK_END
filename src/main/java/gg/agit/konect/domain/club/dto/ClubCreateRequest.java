package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.model.University;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubCreateRequest(
    @NotBlank(message = "동아리 이름은 필수 입력입니다.")
    @Size(max = 50, message = "동아리 이름은 최대 50자까지 입력 가능합니다.")
    @Schema(description = "동아리 이름", example = "BCSD Lab", requiredMode = REQUIRED)
    String name,

    @NotBlank(message = "동아리 설명은 필수 입력입니다.")
    @Size(max = 100, message = "동아리 설명은 최대 100자까지 입력 가능합니다.")
    @Schema(description = "동아리 한 줄 소개", example = "코리아텍 중앙 SW 개발 동아리", requiredMode = REQUIRED)
    String description,

    @NotBlank(message = "동아리 소개는 필수 입력입니다.")
    @Schema(description = "동아리 상세 소개", example = "BCSD Lab은...", requiredMode = REQUIRED)
    String introduce,

    @NotBlank(message = "동아리 이미지 URL은 필수 입력입니다.")
    @Size(max = 255, message = "이미지 URL은 최대 255자까지 입력 가능합니다.")
    @Schema(description = "동아리 대표 이미지 URL", example = "https://example.com/image.png", requiredMode = REQUIRED)
    String imageUrl,

    @NotBlank(message = "동아리 위치는 필수 입력입니다.")
    @Size(max = 255, message = "위치는 최대 255자까지 입력 가능합니다.")
    @Schema(description = "동아리 위치", example = "다솔관 123호", requiredMode = REQUIRED)
    String location,

    @NotNull(message = "동아리 카테고리는 필수 입력입니다.")
    @Schema(description = "동아리 카테고리", example = "IT", requiredMode = REQUIRED)
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
