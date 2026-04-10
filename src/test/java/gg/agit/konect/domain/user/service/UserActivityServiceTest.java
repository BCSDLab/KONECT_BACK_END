package gg.agit.konect.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;

class UserActivityServiceTest extends ServiceTestSupport {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserActivityService userActivityService;

    @Test
    @DisplayName("updateLastLoginAt은 userId가 null이면 저장소를 호출하지 않는다")
    void updateLastLoginAtSkipsWhenUserIdIsNull() {
        // when
        userActivityService.updateLastLoginAt(null);

        // then
        verify(userRepository, never()).getById(null);
    }

    @Test
    @DisplayName("updateLastLoginAt은 마지막 로그인/활동 시각을 함께 갱신한다")
    void updateLastLoginAtUpdatesLoginAndActivityTimestamp() {
        // given
        User user = createUser(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);

        // when
        userActivityService.updateLastLoginAt(1);

        // then
        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(user.getLastActivityAt()).isEqualTo(user.getLastLoginAt());
    }

    @Test
    @DisplayName("updateLastActivityAt은 사용자가 존재할 때만 마지막 활동 시각을 갱신한다")
    void updateLastActivityAtUpdatesExistingUserOnly() {
        // given
        User user = createUser(2, "2021136002");
        LocalDateTime loginAt = LocalDateTime.of(2026, 4, 1, 9, 0);
        user.updateLastLoginAt(loginAt);
        given(userRepository.findById(2)).willReturn(Optional.of(user));

        // when
        userActivityService.updateLastActivityAt(2);

        // then
        assertThat(user.getLastLoginAt()).isEqualTo(loginAt);
        assertThat(user.getLastActivityAt()).isAfterOrEqualTo(loginAt);
    }

    @Test
    @DisplayName("updateLastActivityAt은 사용자가 없거나 userId가 null이면 조용히 종료한다")
    void updateLastActivityAtSkipsWhenUserMissingOrNull() {
        // given
        given(userRepository.findById(3)).willReturn(Optional.empty());

        // when
        userActivityService.updateLastActivityAt(3);
        userActivityService.updateLastActivityAt(null);

        // then
        verify(userRepository).findById(3);
        verify(userRepository, never()).findById(null);
    }

    private User createUser(Integer id, String studentNumber) {
        University university = UniversityFixture.create();
        return User.builder()
            .id(id)
            .university(university)
            .email(studentNumber + "@koreatech.ac.kr")
            .name("테스트유저" + id)
            .studentNumber(studentNumber)
            .role(UserRole.USER)
            .isMarketingAgreement(true)
            .imageUrl("https://example.com/profile.png")
            .build();
    }
}
