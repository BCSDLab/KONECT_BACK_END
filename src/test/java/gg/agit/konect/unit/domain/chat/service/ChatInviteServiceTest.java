package gg.agit.konect.unit.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import gg.agit.konect.domain.chat.dto.ChatInvitableUsersResponse;
import gg.agit.konect.domain.chat.enums.ChatInviteSortBy;
import gg.agit.konect.domain.chat.repository.ChatInviteQueryRepository;
import gg.agit.konect.domain.chat.service.ChatInviteService;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatInviteServiceTest extends ServiceTestSupport {

    @Mock
    private ChatInviteQueryRepository chatInviteQueryRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("이름 정렬은 초대 후보를 단일 사용자 목록으로 변환한다")
    void getInvitableUsersReturnsNameSortedUsers() {
        // given
        Integer userId = 10;
        User requester = createUser(userId, "요청자");
        User candidate = createUser(20, "후보");
        PageRequest pageRequest = PageRequest.of(0, 20);
        ChatInviteService service = new ChatInviteService(chatInviteQueryRepository, userRepository);

        given(userRepository.getById(userId)).willReturn(requester);
        given(chatInviteQueryRepository.findInvitableUsers(userId, "후보", pageRequest))
            .willReturn(new PageImpl<>(List.of(candidate), pageRequest, 1));

        // when
        ChatInvitableUsersResponse response = service.getInvitableUsers(
            userId,
            "후보",
            ChatInviteSortBy.NAME,
            1,
            20
        );

        // then
        assertThat(response.sortBy()).isEqualTo(ChatInviteSortBy.NAME);
        assertThat(response.grouped()).isFalse();
        assertThat(response.users())
            .extracting(ChatInvitableUsersResponse.InvitableUser::userId)
            .containsExactly(candidate.getId());
        assertThat(response.sections()).isEmpty();
    }

    @Test
    @DisplayName("동아리 정렬은 현재 페이지 후보만 대표 동아리 섹션으로 묶는다")
    void getInvitableUsersReturnsClubSections() {
        // given
        Integer userId = 10;
        User requester = createUser(userId, "요청자");
        User bcsdUser = createUser(20, "BCSD 후보");
        User etcUser = createUser(30, "기타 후보");
        Club bcsd = ClubFixture.createWithId(UniversityFixture.create(), 1, "BCSD");
        ClubMember bcsdMembership = ClubMemberFixture.createMember(bcsd, bcsdUser);
        PageRequest pageRequest = PageRequest.of(0, 20);
        ChatInviteService service = new ChatInviteService(chatInviteQueryRepository, userRepository);

        given(userRepository.getById(userId)).willReturn(requester);
        given(chatInviteQueryRepository.findInvitableUserIdsGroupedByClub(userId, null, pageRequest))
            .willReturn(new PageImpl<>(List.of(bcsdUser.getId(), etcUser.getId()), pageRequest, 2));
        given(userRepository.findAllByIdIn(List.of(bcsdUser.getId(), etcUser.getId())))
            .willReturn(List.of(etcUser, bcsdUser));
        given(chatInviteQueryRepository.findSharedClubMemberships(userId, List.of(bcsdUser.getId(), etcUser.getId())))
            .willReturn(List.of(bcsdMembership));

        // when
        ChatInvitableUsersResponse response = service.getInvitableUsers(
            userId,
            null,
            ChatInviteSortBy.CLUB,
            1,
            20
        );

        // then
        assertThat(response.sortBy()).isEqualTo(ChatInviteSortBy.CLUB);
        assertThat(response.grouped()).isTrue();
        assertThat(response.users()).isEmpty();
        assertThat(response.sections())
            .extracting(ChatInvitableUsersResponse.InvitableSection::clubName)
            .containsExactly("BCSD", "기타");
        assertThat(response.sections().get(0).users())
            .extracting(ChatInvitableUsersResponse.InvitableUser::userId)
            .containsExactly(bcsdUser.getId());
        assertThat(response.sections().get(1).users())
            .extracting(ChatInvitableUsersResponse.InvitableUser::userId)
            .containsExactly(etcUser.getId());
        verify(userRepository).findAllByIdIn(List.of(bcsdUser.getId(), etcUser.getId()));
    }

    private User createUser(Integer id, String name) {
        return UserFixture.createUserWithId(
            UniversityFixture.create(),
            id,
            name,
            "2024" + String.format("%04d", id),
            UserRole.USER
        );
    }
}
