package gg.agit.konect.global.auth.oauth;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AppleOAuthNameResolver {

    public String resolve(Map<String, Object> claims) {
        String name = asText(claims.get("name"));

        if (StringUtils.hasText(name)) {
            return name;
        }

        String givenName = asText(claims.get("given_name"));
        String familyName = asText(claims.get("family_name"));

        if (StringUtils.hasText(givenName) && StringUtils.hasText(familyName)) {
            return familyName + givenName;
        }

        if (StringUtils.hasText(givenName)) {
            return givenName;
        }

        if (StringUtils.hasText(familyName)) {
            return familyName;
        }

        Object rawName = claims.get("name");

        if (!(rawName instanceof Map<?, ?> nameMap)) {
            return null;
        }

        String firstName = asText(nameMap.get("firstName"));
        String lastName = asText(nameMap.get("lastName"));

        if (StringUtils.hasText(firstName) && StringUtils.hasText(lastName)) {
            return lastName + firstName;
        }

        if (StringUtils.hasText(firstName)) {
            return firstName;
        }

        if (StringUtils.hasText(lastName)) {
            return lastName;
        }

        return null;
    }

    private String asText(Object value) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
        }

        return null;
    }
}
