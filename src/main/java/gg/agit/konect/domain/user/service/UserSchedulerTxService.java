package gg.agit.konect.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSchedulerTxService {

    private final UserOAuthAccountRepository userOAuthAccountRepository;

    @Transactional(readOnly = true)
    public List<UserOAuthAccount> findAccountsToRevoke(LocalDateTime threshold) {
        return userOAuthAccountRepository.findAppleAccountsToRevoke(Provider.APPLE, threshold);
    }

    @Transactional
    public void clearAppleRefreshTokenIfMatches(Integer accountId, String expectedRefreshToken) {
        userOAuthAccountRepository.findById(accountId)
            .filter(account -> expectedRefreshToken.equals(account.getAppleRefreshToken()))
            .ifPresent(account -> {
                account.updateAppleRefreshToken(null);
                userOAuthAccountRepository.save(account);
            });
    }
}
