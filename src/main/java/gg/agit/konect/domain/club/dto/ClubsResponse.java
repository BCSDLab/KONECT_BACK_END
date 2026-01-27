package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubsResponse(
    @Schema(description = "조건에 해당하는 동아리 수", example = "10", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 동아리 수", example = "5", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "2", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "동아리 리스트", requiredMode = REQUIRED)
    List<InnerClubResponse> clubs
) {
    public record InnerClubResponse(
        @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "동아리 이름", example = "BCSD", requiredMode = REQUIRED)
        String name,

        @Schema(description = "동아리 대표 링크", example = "https://bcsdlab.com/static/img/logo.d89d9cc.png", requiredMode = REQUIRED)
        String imageUrl,

        @Schema(description = "동아리 분과", example = "학술", requiredMode = REQUIRED)
        String categoryName,

        @Schema(description = "동아리 소개", example = "즐겁게 일하고 열심히 노는 IT 특성화 동아리", requiredMode = REQUIRED)
        String description,

        @Schema(description = "동아리 모집 상태", example = "ONGOING", requiredMode = REQUIRED)
        RecruitmentStatus status,

        @Schema(description = "가입 승인 대기 중 여부", example = "false", requiredMode = REQUIRED)
        Boolean isApplied,

        @Schema(description = "상시 모집 여부", example = "false", requiredMode = REQUIRED)
        Boolean isAlwaysRecruiting,

        @Schema(description = "지원 마감일", example = "2025.12.31", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd")
        LocalDate applicationDeadline,

        @Schema(description = "동아리 태그 리스트", example = "[\"IT\", \"프로그래밍\"]", requiredMode = REQUIRED)
        List<String> tags
    ) {
        public static InnerClubResponse from(ClubSummaryInfo clubSummaryInfo, boolean isApplied) {
            return new InnerClubResponse(
                clubSummaryInfo.id(),
                clubSummaryInfo.name(),
                clubSummaryInfo.imageUrl(),
                clubSummaryInfo.categoryName(),
                clubSummaryInfo.description(),
                clubSummaryInfo.status(),
                isApplied,
                clubSummaryInfo.isAlwaysRecruiting(),
                clubSummaryInfo.applicationDeadline(),
                clubSummaryInfo.tags()
            );
        }
    }

    public static ClubsResponse of(Page<ClubSummaryInfo> page) {
        return of(page, Set.of());
    }

    public static ClubsResponse of(Page<ClubSummaryInfo> page, Set<Integer> pendingAppliedClubIds) {
        return new ClubsResponse(
            page.getTotalElements(),
            page.getNumberOfElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            page.stream()
                .map(club -> InnerClubResponse.from(club, pendingAppliedClubIds.contains(club.id())))
                .toList()
        );
    }
}
