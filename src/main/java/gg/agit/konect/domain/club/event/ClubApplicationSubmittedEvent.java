package gg.agit.konect.domain.club.event;

import java.util.List;

public record ClubApplicationSubmittedEvent(
    List<Integer> receiverIds,
    Integer applicationId,
    Integer clubId,
    String clubName,
    String applicantName
) {
    public static ClubApplicationSubmittedEvent of(
        List<Integer> receiverIds,
        Integer applicationId,
        Integer clubId,
        String clubName,
        String applicantName
    ) {
        return new ClubApplicationSubmittedEvent(receiverIds, applicationId, clubId, clubName, applicantName);
    }
}
