package gg.agit.konect.domain.website.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.university.model.University;
import io.swagger.v3.oas.annotations.media.Schema;

public record WebsiteClubsResponse(
    @Schema(description = "대학 정보", requiredMode = REQUIRED)
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
        String imageUrl
    ) {
        public static UniversityResponse from(University university) {
            if (university == null) {
                return null;
            }

            return new UniversityResponse(
                university.getId(),
                university.getKoreanName(),
                university.getCampus().getDisplayName(),
                university.getRegion(),
                university.getRegion().getDisplayName(),
                university.getImageUrl()
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

        @Schema(description = "등록 회원 수", example = "31", requiredMode = REQUIRED)
        Long memberCount
    ) {
        public static ClubResponse of(Club club, Long memberCount) {
            return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getImageUrl(),
                club.getClubCategory(),
                club.getClubCategory().getDescription(),
                club.getTopic(),
                club.getDescription(),
                memberCount
            );
        }
    }

    public static WebsiteClubsResponse of(
        University university,
        Page<Club> page,
        Map<Integer, Long> memberCounts,
        Map<ClubCategory, Long> categoryCounts
    ) {
        return new WebsiteClubsResponse(
            UniversityResponse.from(university),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            createCategories(categoryCounts),
            createClubs(page.getContent(), memberCounts)
        );
    }

    public static WebsiteClubsResponse recent(List<Club> clubs, Map<Integer, Long> memberCounts) {
        return new WebsiteClubsResponse(
            null,
            (long)clubs.size(),
            1,
            1,
            List.of(),
            createClubs(clubs, memberCounts)
        );
    }

    private static List<CategoryCountResponse> createCategories(Map<ClubCategory, Long> categoryCounts) {
        return Arrays.stream(ClubCategory.values())
            .map(category -> CategoryCountResponse.of(category, categoryCounts.getOrDefault(category, 0L)))
            .toList();
    }

    private static List<ClubResponse> createClubs(List<Club> clubs, Map<Integer, Long> memberCounts) {
        return clubs.stream()
            .map(club -> ClubResponse.of(club, memberCounts.getOrDefault(club.getId(), 0L)))
            .toList();
    }
}
