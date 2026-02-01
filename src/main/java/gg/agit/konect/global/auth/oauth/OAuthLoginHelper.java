package gg.agit.konect.global.auth.oauth;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.config.SecurityProperties;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthLoginHelper {

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;
    private final SecurityProperties securityProperties;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public Optional<User> findUserByProvider(Provider provider, String email, String providerId) {
        if (provider == Provider.APPLE) {
            return userRepository.findByProviderIdAndProvider(providerId, provider);
        }
        return userRepository.findByEmailAndProvider(email, provider);
    }

    // Apple 로그인 시 이메일이 누락된 경우, UnRegisteredUser에서 이메일을 조회
    public String resolveAppleEmail(String providerId) {
        if (!StringUtils.hasText(providerId)) {
            return null;
        }

        return unRegisteredUserRepository.findByProviderIdAndProvider(providerId, Provider.APPLE)
            .map(UnRegisteredUser::getEmail)
            .orElse(null);
    }

    // Apple OAuth 인증 성공 후 네이티브 앱으로 복귀시키기 위한 딥링크 여부 확인
    public boolean isAppleOauthCallback(String redirectUri) {
        return redirectUri != null && redirectUri.startsWith("konect://oauth/callback");
    }

    // 리다이렉트 URI에 브릿지 용 토큰을 추가
    public String appendBridgeToken(String redirectUri, String bridgeToken) {
        if (redirectUri.contains("bridge_token=")) {
            return redirectUri;
        }

        char joiner = redirectUri.contains("?") ? '&' : '?';

        return redirectUri + joiner + "bridge_token=" + bridgeToken;
    }

    /*
     * 허용된 리다이렉트 URI를 반환
     * origin 목록에 있는 경우에만 입력된 URI를 반환하고, 그렇지 않은 경우 기본 홈 URL을 반환.
     */
    public String resolveSafeRedirect(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return frontendBaseUrl + "/home";
        }

        Set<String> allowedOrigins = securityProperties.getAllowedRedirectOrigins();

        try {
            URI uri = URI.create(redirectUri);

            if ("konect".equalsIgnoreCase(uri.getScheme()) && "oauth".equalsIgnoreCase(uri.getHost())) {
                return redirectUri;
            }

            if (uri.getScheme() == null || uri.getHost() == null) {
                return frontendBaseUrl + "/home";
            }

            String origin = uri.getScheme() + "://" + uri.getHost() + portPart(uri);

            if (allowedOrigins.contains(origin)) {
                return redirectUri;
            }
        } catch (Exception ignored) {
        }

        return frontendBaseUrl + "/home";
    }

    private String portPart(URI uri) {
        return (uri.getPort() == -1) ? "" : ":" + uri.getPort();
    }
}
