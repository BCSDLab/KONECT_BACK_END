package gg.agit.konect.domain.studytime.model;

public record StudyTimeAggregate(
    long sessionSeconds,
    long dailySeconds,
    long monthlySeconds,
    long totalSeconds
) {
}
