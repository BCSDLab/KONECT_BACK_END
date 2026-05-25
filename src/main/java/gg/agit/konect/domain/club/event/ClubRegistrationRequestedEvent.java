package gg.agit.konect.domain.club.event;

import gg.agit.konect.domain.club.model.ClubRegistrationRequest;

public record ClubRegistrationRequestedEvent(
    Integer requestId,
    String universityName,
    String clubName,
    String category,
    String topic,
    String emoji,
    String description,
    int imageCount
) {

    public static ClubRegistrationRequestedEvent from(ClubRegistrationRequest request) {
        return new ClubRegistrationRequestedEvent(
            request.getId(),
            request.getUniversityName(),
            request.getClubName(),
            request.getClubCategory().getDescription(),
            request.getClubTopic(),
            request.getClubEmoji(),
            request.getShortDescription(),
            request.getImages().size()
        );
    }
}
