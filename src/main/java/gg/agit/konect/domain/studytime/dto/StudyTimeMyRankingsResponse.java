package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimeMyRankingsResponse(
    @Schema(description = "내 동아리 랭킹 리스트", requiredMode = REQUIRED)
    List<StudyTimeRankingResponse> clubRankings,

    @Schema(description = "내 학번 랭킹", requiredMode = NOT_REQUIRED)
    StudyTimeRankingResponse studentNumberRanking,

    @Schema(description = "내 개인 랭킹", requiredMode = NOT_REQUIRED)
    StudyTimeRankingResponse personalRanking
) {

    public static StudyTimeMyRankingsResponse of(
        List<StudyTimeRankingResponse> clubRankings,
        StudyTimeRankingResponse studentNumberRanking,
        StudyTimeRankingResponse personalRanking
    ) {
        return new StudyTimeMyRankingsResponse(clubRankings, studentNumberRanking, personalRanking);
    }
}
