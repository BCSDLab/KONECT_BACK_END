package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimeRankingsResponse(
    @Schema(description = "조건에 해당하는 랭킹 수", example = "120", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 랭킹 수", example = "20", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "6", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "공부 시간 랭킹 리스트", requiredMode = REQUIRED)
    List<InnerStudyTimeRanking> rankings
) {
    public record InnerStudyTimeRanking(
        @Schema(description = "순위", example = "1", requiredMode = REQUIRED)
        Integer rank,

        @Schema(description = "이름(동아리 / 학번 두 자리 / 개인)", example = "BCSD", requiredMode = REQUIRED)
        String name,

        @Schema(description = "이번 달 공부 시간(누적 초)", example = "120000", requiredMode = REQUIRED)
        Long monthlyStudyTime,

        @Schema(description = "오늘 공부 시간(누적 초)", example = "5400", requiredMode = REQUIRED)
        Long dailyStudyTime
    ) {
    }

    public static StudyTimeRankingsResponse of(
        Long totalCount,
        Integer currentCount,
        Integer totalPage,
        Integer currentPage,
        List<InnerStudyTimeRanking> rankings
    ) {
        return new StudyTimeRankingsResponse(totalCount, currentCount, totalPage, currentPage, rankings);
    }
}
