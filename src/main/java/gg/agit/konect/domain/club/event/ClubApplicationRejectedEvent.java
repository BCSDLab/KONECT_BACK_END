package gg.agit.konect.domain.club.event;

public record ClubApplicationRejectedEvent(
    Integer receiverId,
    Integer clubId,
    String clubName
) {
    public static ClubApplicationRejectedEvent of(Integer receiverId, Integer clubId, String clubName) {
        return new ClubApplicationRejectedEvent(receiverId, clubId, clubName);
    }
}
