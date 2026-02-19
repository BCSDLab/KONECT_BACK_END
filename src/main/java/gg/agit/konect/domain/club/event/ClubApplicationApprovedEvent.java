package gg.agit.konect.domain.club.event;

public record ClubApplicationApprovedEvent(
    Integer receiverId,
    Integer clubId,
    String clubName
) {
    public static ClubApplicationApprovedEvent of(Integer receiverId, Integer clubId, String clubName) {
        return new ClubApplicationApprovedEvent(receiverId, clubId, clubName);
    }
}
