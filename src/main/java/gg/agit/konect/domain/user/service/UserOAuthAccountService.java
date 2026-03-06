package gg.agit.konect.domain.user.service;

import java.util.EnumSet;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final EntityManager entityManager;
    private final Environment environment;

    public OAuthLinkStatusResponse getLinkStatus(Integer userId) {
        User user = userRepository.getById(userId);
        EnumSet<Provider> linkedProviders = EnumSet.noneOf(Provider.class);

        if (user.getProvider() != null) {
            linkedProviders.add(user.getProvider());
        }

        List<UserOAuthAccount> accounts = userOAuthAccountRepository.findAllByUserId(userId);
        for (UserOAuthAccount account : accounts) {
            linkedProviders.add(account.getProvider());
        }

        List<OAuthProviderLinkStatus> statuses = List.of(Provider.values()).stream()
            .map(provider -> new OAuthProviderLinkStatus(provider, linkedProviders.contains(provider)))
            .toList();

        return new OAuthLinkStatusResponse(statuses);
    }

    @Transactional
    public void linkOAuthAccount(Integer userId, Provider provider, String providerId, String oauthEmail) {
        User user = userRepository.getById(userId);
        linkAccount(user, provider, providerId, oauthEmail, true);
    }

    @Transactional
    public void linkPrimaryOAuthAccount(User user, Provider provider, String providerId, String oauthEmail) {
        linkAccount(user, provider, providerId, oauthEmail, false);
    }

    @Transactional
    public int cleanupExpiredWithdrawnUserOAuthAccounts() {
        return cleanupExpiredWithdrawnUserOAuthAccounts(LocalDateTime.now());
    }

    @Transactional
    public int cleanupExpiredWithdrawnUserOAuthAccounts(LocalDateTime now) {
        LocalDateTime expiredAt = now.minusDays(RESTORE_WINDOW_DAYS);
        int deletedCount = userOAuthAccountRepository.deleteAllByWithdrawnUsersBefore(expiredAt);
        entityManager.flush();
        return deletedCount;
    }

    private void linkAccount(
        User user,
        Provider provider,
        String providerId,
        String oauthEmail,
        boolean requireProviderId
    ) {
        if (!StringUtils.hasText(providerId)) {
            if (requireProviderId) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID);
            }

            return;
        }

        Integer userId = user.getId();

        restoreOrCleanupWithdrawnByLinkedProvider(provider, providerId);
        validatePrimaryProviderOwnership(user, provider, providerId);
        validateProviderOwnership(userId, provider, providerId);

        UserOAuthAccount account = userOAuthAccountRepository.findByUserIdAndProvider(userId, provider)
            .orElse(null);

        if (account == null) {
            saveWithConflictHandling(userId, user, provider, providerId, oauthEmail);
            return;
        }

        if (!providerId.equals(account.getProviderId())) {
            throw CustomException.of(ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED);
        }

        if (StringUtils.hasText(oauthEmail)) {
            account.updateOauthEmail(oauthEmail);
            userOAuthAccountRepository.save(account);
        }
    }

    private void validateProviderOwnership(Integer userId, Provider provider, String providerId) {
        userOAuthAccountRepository.findUserByProviderAndProviderId(provider, providerId)
            .ifPresent(ownedUser -> {
                if (!ownedUser.getId().equals(userId)) {
                    throw CustomException.of(ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED);
                }
            });
    }

    private void validatePrimaryProviderOwnership(User user, Provider provider, String providerId) {
        Integer userId = user.getId();
        userRepository.findByProviderIdAndProvider(providerId, provider)
            .ifPresent(ownedUser -> {
                if (!ownedUser.getId().equals(userId)) {
                    throw CustomException.of(ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED);
                }
            });

        if (provider.equals(user.getProvider()) && !providerId.equals(user.getProviderId())) {
            throw CustomException.of(ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED);
        }
    }

    private void saveWithConflictHandling(
        Integer userId,
        User user,
        Provider provider,
        String providerId,
        String oauthEmail
    ) {
        try {
            userOAuthAccountRepository.save(UserOAuthAccount.of(user, provider, providerId, oauthEmail));
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            throw CustomException.of(ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED);
        }
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
        entityManager.flush();
    }

    private boolean isStageProfile() {
        return environment.acceptsProfiles(Profiles.of(STAGE_PROFILE));
    }

}
