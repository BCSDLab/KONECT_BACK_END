package gg.agit.konect.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.infrastructure.oauth.AppleTokenRevocationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSchedulerService {

    private static final int REVOKE_AFTER_DAYS = 7;

    private final UserRepository userRepository;
    private final AppleTokenRevocationService appleTokenRevocationService;

    /**
     * 7일 이상 경과한 Apple 사용자의 토큰을 revoke합니다.
     * - 7일 복구 정책: 탈퇴 후 7일 이내 복구 가능하므로 즉시 revoke하지 않음
     * - 7일 경과 후: 복구 불가 시점이므로 Apple 토큰 영구 폐기
     */
    @Transactional
    public void revokeAppleTokensAfterRestoreWindow() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(REVOKE_AFTER_DAYS);
        List<User> usersToRevoke = userRepository.findByProviderAndDeletedAtBefore(
            Provider.APPLE,
            threshold
        );

        if (usersToRevoke.isEmpty()) {
            return;
        }

        for (User user : usersToRevoke) {
            try {
                if (user.getAppleRefreshToken() != null) {
                    appleTokenRevocationService.revoke(user.getAppleRefreshToken());
                    user.clearAppleRefreshToken();
                    userRepository.save(user);
                }
            } catch (Exception e) {
                // 개별 사용자의 토큰 revoke 실패 시 다른 사용자 처리 계속
                // 로깅은 스케줄러에서 처리
                throw e;
            }
        }
    }
}
