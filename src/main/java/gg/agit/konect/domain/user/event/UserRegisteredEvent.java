package gg.agit.konect.domain.user.event;

public record UserRegisteredEvent(
    String email,
    String provider
) {
    public static UserRegisteredEvent from(String email, String provider) {
        return new UserRegisteredEvent(email, provider);
    }
}
