package gg.agit.konect.domain.club.event;

public record ClubMemberChangedEvent(
    Integer clubId
) {
    public static ClubMemberChangedEvent of(Integer clubId) {
        return new ClubMemberChangedEvent(clubId);
    }
}
