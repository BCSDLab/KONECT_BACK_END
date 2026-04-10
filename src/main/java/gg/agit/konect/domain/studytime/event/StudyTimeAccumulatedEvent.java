package gg.agit.konect.domain.studytime.event;

public record StudyTimeAccumulatedEvent(
    Integer userId
) {
    public static StudyTimeAccumulatedEvent of(Integer userId) {
        return new StudyTimeAccumulatedEvent(userId);
    }
}
