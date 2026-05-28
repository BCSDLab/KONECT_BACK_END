package gg.agit.konect.domain.website.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.model.WebUniversity;
import io.swagger.v3.oas.annotations.media.Schema;

public record WebsiteClubDetailResponse(
    @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer id,

    @Schema(description = "동아리명", example = "BCSD Lab", requiredMode = REQUIRED)
    String name,

    @Schema(description = "동아리 로고 이미지 URL", requiredMode = REQUIRED)
    String imageUrl,

    @Schema(description = "분과 코드", example = "ACADEMIC", requiredMode = REQUIRED)
    ClubCategory category,

    @Schema(description = "분과명", example = "학술", requiredMode = REQUIRED)
    String categoryName,

    @Schema(description = "동아리 주제", example = "코딩", requiredMode = REQUIRED)
    String topic,

    @Schema(description = "한 줄 소개", example = "테스트 동아리 소개", requiredMode = REQUIRED)
    String description,

    @Schema(description = "상세 소개", requiredMode = REQUIRED)
    String introduce,

    @Schema(description = "대학 정보", requiredMode = REQUIRED)
    University university
) {

    @Schema(name = "WebsiteClubDetailUniversityResponse")
    public record University(
        @Schema(description = "대학 고유 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "대학명", example = "한국기술교육대학교", requiredMode = REQUIRED)
        String name,

        @Schema(description = "캠퍼스명", example = "본교", requiredMode = REQUIRED)
        String campusName,

        @Schema(description = "지역 코드", example = "CHUNGCHEONG", requiredMode = REQUIRED)
        UniversityRegion region,

        @Schema(description = "지역명", example = "충청도", requiredMode = REQUIRED)
        String regionName,

        @Schema(
            description = "대학 로고 이미지 URL",
            example = "https://example.com/koreatech-logo.png",
            requiredMode = REQUIRED
        )
        String imageUrl,

        @Schema(description = "대학에 등록된 동아리 수", example = "28", requiredMode = REQUIRED)
        Long clubCount
    ) {
    }

    public static WebsiteClubDetailResponse of(WebClub club, Long universityClubCount) {
        WebUniversity university = club.getUniversity();
        return new WebsiteClubDetailResponse(
            club.getId(),
            club.getName(),
            club.getImageUrl(),
            club.getClubCategory(),
            club.getClubCategory().getDescription(),
            club.getTopic(),
            club.getDescription(),
            club.getIntroduce(),
            new University(
                university.getId(),
                university.getKoreanName(),
                university.getCampus().getDisplayName(),
                university.getRegion(),
                university.getRegion().getDisplayName(),
                university.getImageUrl(),
                universityClubCount
            )
        );
    }
}
