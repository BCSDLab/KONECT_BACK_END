package gg.agit.konect.domain.club.event;

import gg.agit.konect.domain.club.model.ClubInformationUpdateRequest;

public record ClubInformationUpdateRequestedEvent(
    Integer requestId,
    Integer clubId,
    String currentClubName,
    String requestedClubName,
    String currentCategory,
    String requestedCategory,
    String currentDescription,
    String requestedDescription,
    String currentImageUrl,
    String requestedImageUrl,
    String currentLocation,
    String requestedLocation,
    String currentFullIntroduction,
    String requestedFullIntroduction
) {

    public static ClubInformationUpdateRequestedEvent from(ClubInformationUpdateRequest request) {
        return new ClubInformationUpdateRequestedEvent(
            request.getId(),
            request.getClub().getId(),
            request.getClub().getName(),
            request.getClubName(),
            request.getClub().getClubCategory().getDescription(),
            request.getClubCategory().getDescription(),
            request.getClub().getDescription(),
            request.getShortDescription(),
            request.getClub().getImageUrl(),
            request.getImageUrl(),
            request.getClub().getLocation(),
            request.getLocation(),
            request.getClub().getIntroduce(),
            request.getFullIntroduction()
        );
    }
}
