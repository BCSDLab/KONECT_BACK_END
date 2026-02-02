package gg.agit.konect.domain.user.event;

public record UserWithdrawnEvent(
    String email
) {
    public static UserWithdrawnEvent from(String email) {
        return new UserWithdrawnEvent(email);
    }
}
