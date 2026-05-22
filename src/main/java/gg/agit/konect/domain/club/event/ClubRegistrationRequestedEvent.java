package gg.agit.konect.domain.club.event;

import java.util.List;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.ClubRegistrationRequestEntity;

public record ClubRegistrationRequestedEvent(
    String universityName,
    String clubName,
    ClubCategory clubCategory,
    String topic,
    String emoji,
    String description,
    List<String> mediaUrls,
    String introduce
) {

    public static ClubRegistrationRequestedEvent from(ClubRegistrationRequest request) {
        return new ClubRegistrationRequestedEvent(
            request.universityName(),
            request.clubName(),
            request.clubCategory(),
            request.topic(),
            request.emoji(),
            request.description(),
            List.copyOf(request.mediaUrls()),
            request.introduce()
        );
    }

    public static ClubRegistrationRequestedEvent from(ClubRegistrationRequestEntity request) {
        return new ClubRegistrationRequestedEvent(
            request.getUniversityName(),
            request.getClubName(),
            request.getClubCategory(),
            request.getTopic(),
            request.getEmoji(),
            request.getDescription(),
            request.getMediaUrls(),
            request.getIntroduce()
        );
    }
}
