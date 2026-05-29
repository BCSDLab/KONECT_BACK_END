package gg.agit.konect.domain.website.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import org.springframework.data.domain.Page;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.model.WebUniversity;
import io.swagger.v3.oas.annotations.media.Schema;

public record WebsiteClubsResponse(
    @Schema(description = "대학 정보", requiredMode = NOT_REQUIRED)
    UniversityResponse university,

    @Schema(description = "전체 동아리 수", example = "28", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "전체 페이지 수", example = "3", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지 번호", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "분과별 동아리 수", requiredMode = REQUIRED)
    List<CategoryCountResponse> categories,

    @Schema(description = "동아리 목록", requiredMode = REQUIRED)
    List<ClubResponse> clubs
) {

    @Schema(name = "WebsiteClubsUniversityResponse")
    public record UniversityResponse(
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

        @Schema(description = "대학 전체 동아리 수", example = "28", requiredMode = REQUIRED)
        Long clubCount
    ) {
        public static UniversityResponse of(WebUniversity university, Long clubCount) {
            if (university == null) {
                return null;
            }

            return new UniversityResponse(
                university.getId(),
                university.getKoreanName(),
                university.getCampus().getDisplayName(),
                university.getRegion(),
                university.getRegion().getDisplayName(),
                university.getImageUrl(),
                clubCount
            );
        }
    }

    public record CategoryCountResponse(
        @Schema(description = "분과 코드", example = "ACADEMIC", requiredMode = REQUIRED)
        ClubCategory category,

        @Schema(description = "분과명", example = "학술", requiredMode = REQUIRED)
        String categoryName,

        @Schema(description = "동아리 수", example = "5", requiredMode = REQUIRED)
        Long count
    ) {
        public static CategoryCountResponse of(ClubCategory category, Long count) {
            return new CategoryCountResponse(category, category.getDescription(), count);
        }
    }

    public record ClubResponse(
        @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "동아리명", example = "BCSD Lab", requiredMode = REQUIRED)
        String name,

        @Schema(description = "동아리 분과 이모지", requiredMode = REQUIRED)
        String categoryEmoji,

        @Schema(description = "분과 코드", example = "ACADEMIC", requiredMode = REQUIRED)
        ClubCategory category,

        @Schema(description = "분과명", example = "학술", requiredMode = REQUIRED)
        String categoryName,

        @Schema(description = "동아리 주제", example = "코딩", requiredMode = REQUIRED)
        String topic,

        @Schema(description = "한 줄 소개", example = "테스트 동아리 소개", requiredMode = REQUIRED)
        String description
    ) {
        public static ClubResponse from(WebClub club) {
            return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getCategoryEmoji(),
                club.getClubCategory(),
                club.getClubCategory().getDescription(),
                club.getTopic(),
                club.getDescription()
            );
        }
    }

    public static WebsiteClubsResponse of(
        WebUniversity university,
        Page<WebClub> page,
        Long universityClubCount,
        java.util.Map<ClubCategory, Long> categoryCounts
    ) {
        return new WebsiteClubsResponse(
            UniversityResponse.of(university, universityClubCount),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            createCategories(categoryCounts),
            createClubs(page.getContent())
        );
    }

    public static WebsiteClubsResponse recent(List<WebClub> clubs) {
        return new WebsiteClubsResponse(
            null,
            (long)clubs.size(),
            1,
            1,
            List.of(),
            createClubs(clubs)
        );
    }

    private static List<CategoryCountResponse> createCategories(java.util.Map<ClubCategory, Long> categoryCounts) {
        return ClubCategory.sortedForDisplay().stream()
            .map(category -> CategoryCountResponse.of(category, categoryCounts.getOrDefault(category, 0L)))
            .toList();
    }

    private static List<ClubResponse> createClubs(List<WebClub> clubs) {
        return clubs.stream()
            .map(ClubResponse::from)
            .toList();
    }
}
