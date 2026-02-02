package gg.agit.konect.domain.user.enums;

public enum Provider {

    GOOGLE("email"),
    NAVER("response.email"),
    KAKAO("kakao_account.email"),
    APPLE("email");

    private final String emailPath;

    Provider(String emailPath) {
        this.emailPath = emailPath;
    }

    public String getEmailPath() {
        return emailPath;
    }
}
