package gg.agit.konect.domain.website.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.website.model.WebsiteUniversitySummary;
import io.swagger.v3.oas.annotations.media.Schema;

public record WebsiteHomeResponse(
    @Schema(description = "검색 결과 대학 수", example = "28", requiredMode = REQUIRED)
    Integer totalUniversityCount,

    @Schema(description = "지역 목록", requiredMode = REQUIRED)
    List<RegionResponse> regions,

    @Schema(description = "대학 목록", requiredMode = REQUIRED)
    List<UniversityResponse> universities
) {
    public record RegionResponse(
        @Schema(description = "지역 코드", example = "CHUNGCHEONG", requiredMode = REQUIRED)
        UniversityRegion region,

        @Schema(description = "지역명", example = "충청도", requiredMode = REQUIRED)
        String regionName
    ) {
        public static RegionResponse from(UniversityRegion region) {
            return new RegionResponse(region, region.getDisplayName());
        }
    }

    @Schema(name = "WebsiteHomeUniversityResponse")
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

        @Schema(description = "등록 동아리 수", example = "31", requiredMode = REQUIRED)
        Long clubCount
    ) {
        public static UniversityResponse from(WebsiteUniversitySummary summary) {
            return new UniversityResponse(
                summary.id(),
                summary.name(),
                summary.campusName(),
                summary.region(),
                summary.regionName(),
                summary.imageUrl(),
                summary.clubCount()
            );
        }
    }

    public static WebsiteHomeResponse from(List<WebsiteUniversitySummary> summaries) {
        return new WebsiteHomeResponse(
            summaries.size(),
            createRegions(),
            summaries.stream()
                .map(UniversityResponse::from)
                .toList()
        );
    }

    private static List<RegionResponse> createRegions() {
        return UniversityRegion.sortedForDisplay().stream()
            .map(RegionResponse::from)
            .toList();
    }
}
