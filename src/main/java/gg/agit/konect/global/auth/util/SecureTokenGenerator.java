package gg.agit.konect.global.auth.util;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class SecureTokenGenerator {

    private static final int DEFAULT_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return generate(DEFAULT_TOKEN_BYTES);
    }

    public String generate(int tokenBytes) {
        if (tokenBytes <= 0) {
            throw new IllegalArgumentException("tokenBytes must be positive");
        }

        byte[] bytes = new byte[tokenBytes];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
