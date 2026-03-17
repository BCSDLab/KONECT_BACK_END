package gg.agit.konect.domain.club.event;

public record ClubFeePaymentApprovedEvent(
    Integer clubId
) {
    public static ClubFeePaymentApprovedEvent of(Integer clubId) {
        return new ClubFeePaymentApprovedEvent(clubId);
    }
}
