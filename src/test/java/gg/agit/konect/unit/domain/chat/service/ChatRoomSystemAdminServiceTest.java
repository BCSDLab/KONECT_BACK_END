package gg.agit.konect.unit.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.service.ChatRoomSystemAdminService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatRoomSystemAdminServiceTest extends ServiceTestSupport {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private ChatRoomSystemAdminService chatRoomSystemAdminService;

    @Test
    @DisplayName("isSystemAdminRoom은 SYSTEM_ADMIN 멤버가 있으면 true를 반환한다")
    void isSystemAdminRoomReturnsTrueWhenSystemAdminMemberExists() {
        given(chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(List.of(1)))
            .willReturn(List.<Object[]>of(new Object[] {1, SYSTEM_ADMIN_ID, LocalDateTime.now()}));

        boolean result = chatRoomSystemAdminService.isSystemAdminRoom(1);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("findSystemAdminMember는 멤버 목록에서 SYSTEM_ADMIN 멤버를 반환한다")
    void findSystemAdminMemberReturnsSystemAdminMember() {
        ChatRoom room = ChatRoom.directOf();
        User systemAdmin = UserFixture.createUserWithId(
            UniversityFixture.createWithId(1),
            SYSTEM_ADMIN_ID,
            "시스템관리자",
            "20240001",
            UserRole.ADMIN
        );
        ChatRoomMember member = ChatRoomMember.of(room, systemAdmin, LocalDateTime.now());

        ChatRoomMember result = chatRoomSystemAdminService.findSystemAdminMember(List.of(member));

        assertThat(result).isSameAs(member);
    }
}
