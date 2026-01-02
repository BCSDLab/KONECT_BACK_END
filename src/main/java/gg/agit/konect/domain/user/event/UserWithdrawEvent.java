package gg.agit.konect.domain.user.event;

public record UserWithdrawEvent(
    String email
) {
    public static UserWithdrawEvent from(String email) {
        return new UserWithdrawEvent(email);
    }
}
