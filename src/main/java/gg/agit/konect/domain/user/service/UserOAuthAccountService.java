package gg.agit.konect.domain.user.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.dto.OAuthLinkStatusResponse;
import gg.agit.konect.domain.user.dto.OAuthProviderLinkStatus;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserOAuthAccountService {

    private static final long RESTORE_WINDOW_DAYS = 7L;
    private static final String STAGE_PROFILE = "stage";

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final Environment environment;

    public OAuthLinkStatusResponse getLinkStatus(Integer userId) {
        userRepository.getById(userId);

        List<UserOAuthAccount> accounts = userOAuthAccountRepository.findAllByUserId(userId);
        EnumSet<Provider> linkedProviders = EnumSet.noneOf(Provider.class);

        for (UserOAuthAccount account : accounts) {
            linkedProviders.add(account.getProvider());
        }

        List<OAuthProviderLinkStatus> statuses = List.of(Provider.values()).stream()
            .map(provider -> new OAuthProviderLinkStatus(provider, linkedProviders.contains(provider)))
            .toList();

        return new OAuthLinkStatusResponse(statuses);
    }

    @Transactional
    public void linkOAuthAccount(
        Integer userId,
        Provider provider,
        String providerId,
        String oauthEmail,
        String appleRefreshToken
    ) {
        User user = userRepository.getById(userId);
        linkAccount(user, provider, providerId, oauthEmail, appleRefreshToken, true);
    }

    @Transactional
    public void linkPrimaryOAuthAccount(
        User user,
        Provider provider,
        String providerId,
        String oauthEmail,
        String appleRefreshToken
    ) {
        linkAccount(user, provider, providerId, oauthEmail, appleRefreshToken, false);
    }

    @Transactional
    public int cleanupExpiredWithdrawnUserOAuthAccounts() {
        return cleanupExpiredWithdrawnUserOAuthAccounts(LocalDateTime.now());
    }

    @Transactional
    public int cleanupExpiredWithdrawnUserOAuthAccounts(LocalDateTime now) {
        LocalDateTime expiredAt = now.minusDays(RESTORE_WINDOW_DAYS);
        int deletedCount = userOAuthAccountRepository.deleteAllByWithdrawnUsersBefore(expiredAt);
        userOAuthAccountRepository.flush();
        return deletedCount;
    }

    private void linkAccount(
        User user,
        Provider provider,
        String providerId,
        String oauthEmail,
        String appleRefreshToken,
        boolean requireProviderId
    ) {
        if (!StringUtils.hasText(providerId)) {
            if (requireProviderId) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID);
            }
            // providerId가 없어도 계속 진행 (이메일 기반 매칭)
        }

        Integer userId = user.getId();

        if (StringUtils.hasText(providerId)) {
            restoreOrCleanupWithdrawnByLinkedProvider(provider, providerId);
            validatePrimaryProviderOwnership(user, provider, providerId);
        }

        if (StringUtils.hasText(oauthEmail)) {
            restoreOrCleanupWithdrawnByOauthEmail(provider, oauthEmail);
            validateOauthEmailOwnership(user, provider, oauthEmail);
        }

        UserOAuthAccount account = userOAuthAccountRepository.findByUserIdAndProvider(userId, provider)
            .orElse(null);

        if (account == null) {
            userOAuthAccountRepository.save(
                UserOAuthAccount.of(user, provider, providerId, oauthEmail, appleRefreshToken)
            );
            return;
        }

        // 기존 계정이 있으면 provider_id 업데이트 (NULL -> 값 채우기)
        if (StringUtils.hasText(providerId)) {
            if (!providerId.equals(account.getProviderId())) {
                // provider_id가 NULL이거나 다른 값이면 업데이트
                if (account.getProviderId() == null || !StringUtils.hasText(account.getProviderId())) {
                    account.updateProviderId(providerId);
                } else {
                    // 이미 다른 provider_id가 있으면 충돌
                    throw CustomException.of(ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED);
                }
            }
        }

        if (StringUtils.hasText(oauthEmail)) {
            account.updateOauthEmail(oauthEmail);
        }

        if (provider == Provider.APPLE && StringUtils.hasText(appleRefreshToken)) {
            account.updateAppleRefreshToken(appleRefreshToken);
        }

        userOAuthAccountRepository.save(account);
    }

    // 다른 활성 사용자가 같은 providerId를 이미 연동한 경우를 사전에 막는다.
    private void validatePrimaryProviderOwnership(User user, Provider provider, String providerId) {
        Integer userId = user.getId();

        userOAuthAccountRepository.findUserByProviderAndProviderId(provider, providerId)
            .ifPresent(ownedUser -> {
                if (!ownedUser.getId().equals(userId)) {
                    throw CustomException.of(ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED);
                }
            });
    }

    // 다른 활성 사용자가 같은 소셜 이메일을 이미 연동한 경우를 사전에 막는다.
    private void validateOauthEmailOwnership(User user, Provider provider, String oauthEmail) {
        Integer userId = user.getId();

        userOAuthAccountRepository.findUserByOauthEmailAndProvider(oauthEmail, provider)
            .ifPresent(ownedUser -> {
                if (!ownedUser.getId().equals(userId)) {
                    throw CustomException.of(ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED);
                }
            });
    }

    private void restoreOrCleanupWithdrawnByLinkedProvider(Provider provider, String providerId) {
        Optional<UserOAuthAccount> linkedAccount = userOAuthAccountRepository
            .findAccountByProviderAndProviderId(provider, providerId);

        if (linkedAccount.isEmpty()) {
            return;
        }

        User linkedUser = linkedAccount.get().getUser();
        if (linkedUser.getDeletedAt() == null) {
            return;
        }

        if (!isStageProfile() && linkedUser.canRestore(LocalDateTime.now(), RESTORE_WINDOW_DAYS)) {
            linkedUser.restore();
            userRepository.save(linkedUser);
            return;
        }

        userOAuthAccountRepository.delete(linkedAccount.get());
        userOAuthAccountRepository.flush();
    }

    private void restoreOrCleanupWithdrawnByOauthEmail(Provider provider, String oauthEmail) {
        Optional<UserOAuthAccount> linkedAccount = userOAuthAccountRepository
            .findAccountByProviderAndOauthEmail(provider, oauthEmail);

        if (linkedAccount.isEmpty()) {
            return;
        }

        User linkedUser = linkedAccount.get().getUser();
        if (linkedUser.getDeletedAt() == null) {
            return;
        }

        if (!isStageProfile() && linkedUser.canRestore(LocalDateTime.now(), RESTORE_WINDOW_DAYS)) {
            linkedUser.restore();
            userRepository.save(linkedUser);
            return;
        }

        userOAuthAccountRepository.delete(linkedAccount.get());
        userOAuthAccountRepository.flush();
    }

    private boolean isStageProfile() {
        return environment.acceptsProfiles(Profiles.of(STAGE_PROFILE));
    }

    public UserOAuthAccount getPrimaryOAuthAccount(Integer userId) {
        List<UserOAuthAccount> accounts = userOAuthAccountRepository.findAllByUserId(userId);
        return accounts.isEmpty() ? null : accounts.get(0);
    }
}
