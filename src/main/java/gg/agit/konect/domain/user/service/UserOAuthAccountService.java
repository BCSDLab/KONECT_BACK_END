package gg.agit.konect.domain.user.service;

import java.util.EnumSet;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final EntityManager entityManager;

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
        if (!StringUtils.hasText(providerId)) {
            throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID);
        }

        User user = userRepository.getById(userId);

        userOAuthAccountRepository.findUserByProviderAndProviderId(provider, providerId)
            .ifPresent(ownedUser -> {
                if (!ownedUser.getId().equals(userId)) {
                    throw CustomException.of(ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED);
                }
            });

        UserOAuthAccount account = userOAuthAccountRepository.findByUserIdAndProvider(userId, provider)
            .orElse(null);

        if (account == null) {
            try {
                userOAuthAccountRepository.save(UserOAuthAccount.of(user, provider, providerId, oauthEmail));
                entityManager.flush();
            } catch (DataIntegrityViolationException e) {
                resolveLinkConflictOnIntegrityViolation(userId, provider, providerId);
            }
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

    private void resolveLinkConflictOnIntegrityViolation(Integer userId, Provider provider, String providerId) {
        userOAuthAccountRepository.findUserByProviderAndProviderId(provider, providerId)
            .ifPresent(ownedUser -> {
                if (!ownedUser.getId().equals(userId)) {
                    throw CustomException.of(ApiResponseCode.OAUTH_ACCOUNT_ALREADY_LINKED);
                }
            });

        userOAuthAccountRepository.findByUserIdAndProvider(userId, provider)
            .ifPresent(linkedAccount -> {
                if (!providerId.equals(linkedAccount.getProviderId())) {
                    throw CustomException.of(ApiResponseCode.OAUTH_PROVIDER_ALREADY_LINKED);
                }
            });
    }

    @Transactional
    public void linkPrimaryOAuthAccount(User user, Provider provider, String providerId, String oauthEmail) {
        if (!StringUtils.hasText(providerId)) {
            return;
        }

        userOAuthAccountRepository.findByUserIdAndProvider(user.getId(), provider)
            .ifPresentOrElse(account -> {
                    if (StringUtils.hasText(oauthEmail)) {
                        account.updateOauthEmail(oauthEmail);
                        userOAuthAccountRepository.save(account);
                    }
                },
                () -> userOAuthAccountRepository.save(UserOAuthAccount.of(user, provider, providerId, oauthEmail))
            );
    }
}
