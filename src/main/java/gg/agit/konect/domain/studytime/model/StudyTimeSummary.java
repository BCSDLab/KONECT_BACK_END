package gg.agit.konect.domain.studytime.model;

public record StudyTimeSummary(
    long sessionSeconds,
    long dailySeconds,
    long monthlySeconds,
    long totalSeconds
) {
}
