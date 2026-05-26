package gg.agit.konect.domain.club.event;

import gg.agit.konect.domain.club.model.ClubInformationUpdateRequest;

public record ClubInformationUpdateRequestedEvent(
    Integer requestId,
    Integer clubId,
    String currentClubName,
    String requestedClubName,
    String category,
    String description,
    String imageUrl,
    String location,
    String fullIntroduction
) {

    public static ClubInformationUpdateRequestedEvent from(ClubInformationUpdateRequest request) {
        return new ClubInformationUpdateRequestedEvent(
            request.getId(),
            request.getClub().getId(),
            request.getClub().getName(),
            request.getClubName(),
            request.getClubCategory().getDescription(),
            request.getShortDescription(),
            request.getImageUrl(),
            request.getLocation(),
            request.getFullIntroduction()
        );
    }
}
