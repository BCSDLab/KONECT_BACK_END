package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimeRankingResponse(
    @Schema(description = "순위", example = "1", requiredMode = REQUIRED)
    Integer rank,

    @Schema(description = "이름(동아리 / 학번 앞 네 자리 / 개인)", example = "BCSD", requiredMode = REQUIRED)
    String name,

    @Schema(description = "이번 달 공부 시간(누적 초)", example = "120000", requiredMode = REQUIRED)
    Long monthlyStudyTime,

    @Schema(description = "오늘 공부 시간(누적 초)", example = "5400", requiredMode = REQUIRED)
    Long dailyStudyTime
) {
    public static StudyTimeRankingResponse from(StudyTimeRanking ranking, Integer rank) {
        return new StudyTimeRankingResponse(
            rank,
            ranking.getTargetName(),
            ranking.getMonthlySeconds(),
            ranking.getDailySeconds()
        );
    }
}
