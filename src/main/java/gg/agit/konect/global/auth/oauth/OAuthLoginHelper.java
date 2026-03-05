package gg.agit.konect.global.auth.oauth;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.config.SecurityProperties;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthLoginHelper {

    private static final long RESTORE_WINDOW_DAYS = 7L;

    private static final String STAGE_PROFILE = "stage";

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;
    private final SecurityProperties securityProperties;
    private final Environment environment;
    private final EntityManager entityManager;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional
    public Optional<User> findUserByProvider(Provider provider, String email, String providerId) {
        if (StringUtils.hasText(providerId)) {
            Optional<User> linkedUser = userOAuthAccountRepository
                .findUserByProviderAndProviderId(provider, providerId);
            if (linkedUser.isPresent()) {
                ensureLinkedAccount(linkedUser.get(), provider, providerId, email, linkedUser);
                return linkedUser;
            }

            Optional<User> restoredByLinkedAccount = restoreOrCleanupWithdrawnByLinkedProvider(provider, providerId);
            if (restoredByLinkedAccount.isPresent()) {
                ensureLinkedAccount(
                    restoredByLinkedAccount.get(),
                    provider,
                    providerId,
                    email,
                    restoredByLinkedAccount
                );
                return restoredByLinkedAccount;
            }
        }

        if (provider == Provider.APPLE) {
            Optional<User> user = userRepository.findByProviderIdAndProvider(providerId, provider);
            if (user.isPresent()) {
                ensureLinkedAccount(user.get(), provider, providerId, email);
                return user;
            }

            if (StringUtils.hasText(providerId)) {
                Optional<User> restoredByProviderId = restoreWithdrawnByProviderId(provider, providerId);
                if (restoredByProviderId.isPresent()) {
                    return restoredByProviderId;
                }
            }

            return restoreWithdrawnByEmail(provider, email);
        }

        Optional<User> user = userRepository.findByEmailAndProvider(email, provider);
        if (user.isPresent()) {
            ensureLinkedAccount(user.get(), provider, providerId, email);
            return user;
        }

        return restoreWithdrawnByEmail(provider, email);
    }

    private Optional<User> restoreWithdrawnByProviderId(Provider provider, String providerId) {
        if (isStageProfile()) {
            return Optional.empty();
        }

        return userRepository
            .findFirstByProviderIdAndProviderAndDeletedAtIsNotNullOrderByDeletedAtDesc(providerId, provider)
            .filter(user -> user.canRestore(LocalDateTime.now(), RESTORE_WINDOW_DAYS))
            .map(user -> {
                user.restore();
                return userRepository.save(user);
            });
    }

    private Optional<User> restoreWithdrawnByEmail(Provider provider, String email) {
        if (isStageProfile()) {
            return Optional.empty();
        }

        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }

        return userRepository.findFirstByEmailAndProviderAndDeletedAtIsNotNullOrderByDeletedAtDesc(email, provider)
            .filter(user -> user.canRestore(LocalDateTime.now(), RESTORE_WINDOW_DAYS))
            .map(user -> {
                user.restore();
                return userRepository.save(user);
            });
    }

    private boolean isStageProfile() {
        return environment.acceptsProfiles(Profiles.of(STAGE_PROFILE));
    }

    private Optional<User> restoreOrCleanupWithdrawnByLinkedProvider(Provider provider, String providerId) {
        Optional<UserOAuthAccount> linkedAccount = userOAuthAccountRepository
            .findAccountByProviderAndProviderId(provider, providerId);

        if (linkedAccount.isEmpty()) {
            return Optional.empty();
        }

        User linkedUser = linkedAccount.get().getUser();

        if (linkedUser.getDeletedAt() == null) {
            return Optional.empty();
        }

        if (!isStageProfile() && linkedUser.canRestore(LocalDateTime.now(), RESTORE_WINDOW_DAYS)) {
            linkedUser.restore();
            return Optional.of(userRepository.save(linkedUser));
        }

        userOAuthAccountRepository.delete(linkedAccount.get());
        return Optional.empty();
    }

    private void ensureLinkedAccount(User user, Provider provider, String providerId, String oauthEmail) {
        ensureLinkedAccount(user, provider, providerId, oauthEmail, Optional.empty());
    }

    private void ensureLinkedAccount(
        User user,
        Provider provider,
        String providerId,
        String oauthEmail,
        Optional<User> knownOwner
    ) {
        if (!StringUtils.hasText(providerId)) {
            return;
        }

        Optional<User> owner = knownOwner.isPresent()
            ? knownOwner
            : userOAuthAccountRepository.findUserByProviderAndProviderId(provider, providerId);
        if (owner.isPresent() && !owner.get().getId().equals(user.getId())) {
            return;
        }

        Optional<UserOAuthAccount> linked = userOAuthAccountRepository.findByUserIdAndProvider(user.getId(), provider);

        if (linked.isPresent()) {
            UserOAuthAccount account = linked.get();

            if (providerId.equals(account.getProviderId()) && StringUtils.hasText(oauthEmail)) {
                account.updateOauthEmail(oauthEmail);
                userOAuthAccountRepository.save(account);
            }

            return;
        }

        try {
            userOAuthAccountRepository.save(UserOAuthAccount.of(user, provider, providerId, oauthEmail));
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            resolveEnsureLinkedAccountConflictOnIntegrityViolation(user.getId(), provider, providerId, knownOwner);
        }
    }

    private void resolveEnsureLinkedAccountConflictOnIntegrityViolation(
        Integer userId,
        Provider provider,
        String providerId,
        Optional<User> knownOwner
    ) {
        Optional<User> owner = knownOwner.isPresent()
            ? knownOwner
            : userOAuthAccountRepository.findUserByProviderAndProviderId(provider, providerId);

        if (owner.isPresent() && !owner.get().getId().equals(userId)) {
            throw CustomException.of(ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED);
        }

        Optional<UserOAuthAccount> linkedAccount = userOAuthAccountRepository.findByUserIdAndProvider(userId, provider);
        if (linkedAccount.isPresent()) {
            if (!providerId.equals(linkedAccount.get().getProviderId())) {
                throw CustomException.of(ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED);
            }

            return;
        }

        throw CustomException.of(ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED);
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

    public String appendQueryParameter(String redirectUri, String key, String value) {
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        char joiner = redirectUri.contains("?") ? '&' : '?';

        return redirectUri + joiner + key + "=" + encodedValue;
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
        } catch (Exception ignore) {
        }

        return frontendBaseUrl + "/home";
    }

    private String portPart(URI uri) {
        return (uri.getPort() == -1) ? "" : ":" + uri.getPort();
    }
}
