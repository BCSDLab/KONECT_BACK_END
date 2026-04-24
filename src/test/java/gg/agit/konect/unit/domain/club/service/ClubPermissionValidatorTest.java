package gg.agit.konect.unit.domain.club.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.service.ClubPermissionValidator;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UserFixture;

class ClubPermissionValidatorTest extends ServiceTestSupport {

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubPermissionValidator clubPermissionValidator;

    @Test
    @DisplayName("validateLeaderAccess는 동아리 소속이 없는 어드민도 허용한다")
    void validateLeaderAccessAllowsAdminWithoutClubMembership() {
        // given
        Integer clubId = 1;
        User admin = UserFixture.createUserWithId(100, "관리자", UserRole.ADMIN);

        // when & then
        assertThatCode(() -> clubPermissionValidator.validateLeaderAccess(clubId, admin))
            .doesNotThrowAnyException();
        verifyNoInteractions(clubMemberRepository);
    }
}
