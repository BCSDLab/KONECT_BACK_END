package gg.agit.konect.domain.club.event;

import java.util.List;

import gg.agit.konect.domain.club.model.ClubRegistrationRequest;

public record ClubRegistrationRequestedEvent(
    Integer requestId,
    String universityName,
    String clubName,
    String category,
    String topic,
    String emoji,
    String description,
    String fullIntroduction,
    List<String> imageUrls
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
            request.getFullIntroduction(),
            request.getImages().stream()
                .map(image -> image.getImageUrl())
                .toList()
        );
    }

    public int imageCount() {
        return imageUrls.size();
    }
}
