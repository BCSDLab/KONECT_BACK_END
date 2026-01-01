package gg.agit.konect.global.config;

public final class SecurityPaths {

    public static final String[] PUBLIC_PATHS = {
        "/oauth2/**",
        "/login/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/error"
    };

    public static final String[] DENY_PATHS = {};

    private SecurityPaths() {
    }
}
