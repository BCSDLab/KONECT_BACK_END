package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;

public class UnRegisteredUserFixture {

    public static UnRegisteredUser create(String email, Provider provider) {
        return UnRegisteredUser.builder()
            .email(email)
            .provider(provider)
            .providerId(provider.name().toLowerCase() + "_" + email.split("@")[0])
            .name("임시유저")
            .build();
    }

    public static UnRegisteredUser createGoogle(String email) {
        return create(email, Provider.GOOGLE);
    }

    public static UnRegisteredUser createApple(String email) {
        return UnRegisteredUser.builder()
            .email(email)
            .provider(Provider.APPLE)
            .providerId("apple_" + email.split("@")[0])
            .name("애플유저")
            .build();
    }
}
