package gg.agit.konect.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSchedulerTxService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> findUsersToRevoke(LocalDateTime threshold) {
        return userRepository.findByProviderAndDeletedAtBefore(Provider.APPLE, threshold);
    }

    @Transactional
    public void clearAppleRefreshTokenIfMatches(Integer userId, String expectedRefreshToken) {
        userRepository.findByIdIncludingDeleted(userId)
            .filter(user -> expectedRefreshToken.equals(user.getAppleRefreshToken()))
            .ifPresent(User::clearAppleRefreshToken);
    }
}
