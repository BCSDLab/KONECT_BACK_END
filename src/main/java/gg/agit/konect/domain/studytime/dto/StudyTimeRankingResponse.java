package gg.agit.konect.domain.studytime.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudyTimeRankingResponse(
    @Schema(description = "순위", example = "1", requiredMode = REQUIRED)
    Integer rank,

    @Schema(description = "이름(동아리 / 학번(입학연도 뒤 두 자리) / 개인)", example = "BCSD", requiredMode = REQUIRED)
    String name,

    @Schema(description = "이번 달 공부 시간(누적 초)", example = "120000", requiredMode = REQUIRED)
    Long monthlyStudyTime,

    @Schema(description = "오늘 공부 시간(누적 초)", example = "5400", requiredMode = REQUIRED)
    Long dailyStudyTime
) {
    private static final int STUDENT_NUMBER_DISPLAY_LENGTH = 2;

    public static StudyTimeRankingResponse from(StudyTimeRanking ranking, Integer rank) {
        return new StudyTimeRankingResponse(
            rank,
            ranking.getTargetName(),
            ranking.getMonthlySeconds(),
            ranking.getDailySeconds()
        );
    }

    public static StudyTimeRankingResponse from(StudyTimeRanking ranking, Integer rank, String type) {
        return new StudyTimeRankingResponse(
            rank,
            resolveNameByType(ranking.getTargetName(), type),
            ranking.getMonthlySeconds(),
            ranking.getDailySeconds()
        );
    }

    private static String resolveNameByType(String name, String type) {
        if ("PERSONAL".equalsIgnoreCase(type)) {
            return maskPersonalName(name);
        }

        if ("STUDENT_NUMBER".equalsIgnoreCase(type)) {
            return resolveStudentNumberDisplay(name);
        }

        return name;
    }

    private static String resolveStudentNumberDisplay(String studentNumberYear) {
        if (studentNumberYear.length() <= STUDENT_NUMBER_DISPLAY_LENGTH) {
            return studentNumberYear;
        }

        return studentNumberYear.substring(
            studentNumberYear.length() - STUDENT_NUMBER_DISPLAY_LENGTH
        );
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
