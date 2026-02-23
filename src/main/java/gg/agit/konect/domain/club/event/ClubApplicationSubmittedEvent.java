package gg.agit.konect.domain.club.event;

public record ClubApplicationSubmittedEvent(
    Integer receiverId,
    Integer applicationId,
    Integer clubId,
    String clubName,
    String applicantName
) {
    public static ClubApplicationSubmittedEvent of(
        Integer receiverId,
        Integer applicationId,
        Integer clubId,
        String clubName,
        String applicantName
    ) {
        return new ClubApplicationSubmittedEvent(receiverId, applicationId, clubId, clubName, applicantName);
    }
}
