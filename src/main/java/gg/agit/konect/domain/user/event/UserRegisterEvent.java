package gg.agit.konect.domain.user.event;

public record UserRegisterEvent(
    String email
) {
    public static UserRegisterEvent from(String email) {
        return new UserRegisterEvent(email);
    }
}
