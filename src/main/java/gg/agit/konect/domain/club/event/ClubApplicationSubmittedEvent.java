package gg.agit.konect.domain.club.event;

public record ClubApplicationSubmittedEvent(
    Integer receiverId,
    Integer clubId,
    String clubName,
    String applicantName
) {
    public static ClubApplicationSubmittedEvent of(
        Integer receiverId,
        Integer clubId,
        String clubName,
        String applicantName
    ) {
        return new ClubApplicationSubmittedEvent(receiverId, clubId, clubName, applicantName);
    }
}
