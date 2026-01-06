package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;

import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
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
    List<StudyTimeRankingResponse> rankings
) {
    public static StudyTimeRankingsResponse of(
        Long totalCount,
        Integer currentCount,
        Integer totalPage,
        Integer currentPage,
        List<StudyTimeRankingResponse> rankings
    ) {
        return new StudyTimeRankingsResponse(totalCount, currentCount, totalPage, currentPage, rankings);
    }

    public static StudyTimeRankingsResponse from(
        Page<StudyTimeRanking> rankingPage,
        int baseRank,
        String type
    ) {
        return new StudyTimeRankingsResponse(
            rankingPage.getTotalElements(),
            rankingPage.getNumberOfElements(),
            rankingPage.getTotalPages(),
            rankingPage.getNumber() + 1,
            toRankings(baseRank, rankingPage.getContent(), type)
        );
    }

    public static List<StudyTimeRankingResponse> toRankings(
        int baseRank,
        List<StudyTimeRanking> rankings,
        String type
    ) {
        return IntStream.range(0, rankings.size())
            .mapToObj(index -> {
                StudyTimeRanking ranking = rankings.get(index);
                return new StudyTimeRankingResponse(
                    baseRank + index,
                    resolveName(ranking, type),
                    ranking.getMonthlySeconds(),
                    ranking.getDailySeconds()
                );
            })
            .toList();
    }

    private static String resolveName(StudyTimeRanking ranking, String type) {
        String name = ranking.getTargetName();

        if ("PERSONAL".equalsIgnoreCase(type)) {
            return maskPersonalName(name);
        }

        return name;
    }

    private static String maskPersonalName(String name) {
        if (name.length() == 1) {
            return name;
        }

        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }

        return name.charAt(0)
            + "*".repeat(name.length() - 2)
            + name.substring(name.length() - 1);
    }

}
