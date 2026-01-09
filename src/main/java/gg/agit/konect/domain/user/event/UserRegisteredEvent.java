package gg.agit.konect.domain.user.event;

public record UserRegisteredEvent(
    String email
) {
    public static UserRegisteredEvent from(String email) {
        return new UserRegisteredEvent(email);
    }
}
