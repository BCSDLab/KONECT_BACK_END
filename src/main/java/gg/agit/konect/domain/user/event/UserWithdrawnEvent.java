package gg.agit.konect.domain.user.event;

public record UserWithdrawnEvent(
    String email,
    String provider
) {
    public static UserWithdrawnEvent from(String email, String provider) {
        return new UserWithdrawnEvent(email, provider);
    }
}
