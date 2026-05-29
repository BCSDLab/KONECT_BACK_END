package gg.agit.konect.domain.club.event;

import java.util.List;

import gg.agit.konect.domain.club.model.ClubInformationUpdateRequest;

public record ClubInformationUpdateRequestedEvent(
    Integer requestId,
    Integer clubId,
    String currentUniversityName,
    String requestedUniversityName,
    String currentClubName,
    String requestedClubName,
    String currentCategory,
    String requestedCategory,
    String currentTopic,
    String requestedTopic,
    String currentDescription,
    String requestedDescription,
    String currentFullIntroduction,
    String requestedFullIntroduction,
    String currentImageUrl,
    List<String> requestedImageUrls
) {

    public static ClubInformationUpdateRequestedEvent from(ClubInformationUpdateRequest request) {
        return new ClubInformationUpdateRequestedEvent(
            request.getId(),
            request.getClub().getId(),
            request.getClub().getUniversity().getKoreanName(),
            request.getUniversityName(),
            request.getClub().getName(),
            request.getClubName(),
            request.getClub().getClubCategory().getDescription(),
            request.getClubCategory().getDescription(),
            request.getClub().getTopic(),
            request.getClubTopic(),
            request.getClub().getDescription(),
            request.getShortDescription(),
            request.getClub().getIntroduce(),
            request.getFullIntroduction(),
            request.getClub().getImageUrl(),
            request.getImages().stream()
                .map(image -> image.getImageUrl())
                .toList()
        );
    }
}
