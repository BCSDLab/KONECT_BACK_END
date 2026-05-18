package gg.agit.konect.unit.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.service.ChatDirectRoomAccessService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatDirectRoomAccessServiceTest extends ServiceTestSupport {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 4, 27, 10, 0);

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private ChatDirectRoomAccessService chatDirectRoomAccessService;

    @Test
    @DisplayName("접근 가능한 direct room 멤버를 반환한다")
    void getAccessibleMemberReturnsMember() {
        User user = user(10);
        ChatRoom room = room(1);
        ChatRoomMember member = member(room, user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(member));

        ChatRoomMember result = chatDirectRoomAccessService.getAccessibleMember(room, user);

        assertThat(result).isSameAs(member);
    }

    @Test
    @DisplayName("나간 direct room에 새 메시지가 있으면 접근 시 나간 상태를 해제한다")
    void getAccessibleMemberRestoresLeftMemberWhenVisibleMessageExists() {
        User user = user(10);
        ChatRoom room = room(1);
        room.updateLastMessage("새 메시지", BASE_TIME.plusHours(2));
        ChatRoomMember member = member(room, user);
        member.leaveDirectRoom(BASE_TIME.plusHours(1));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(member));

        chatDirectRoomAccessService.getAccessibleMember(room, user);

        assertThat(member.hasLeft()).isFalse();
    }

    @Test
    @DisplayName("나간 direct room에 새 메시지가 없으면 접근을 거부한다")
    void getAccessibleMemberRejectsLeftMemberWithoutVisibleMessage() {
        User user = user(10);
        ChatRoom room = room(1);
        ChatRoomMember member = member(room, user);
        member.leaveDirectRoom(BASE_TIME.plusHours(1));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(member));

        assertThatThrownBy(() -> chatDirectRoomAccessService.getAccessibleMember(room, user))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException)exception).getErrorCode()).isEqualTo(FORBIDDEN_CHAT_ROOM_ACCESS));
    }

    @Test
    @DisplayName("접근 준비는 복원 전 visibleMessageFrom을 반환한다")
    void prepareAccessAndGetVisibleMessageFromReturnsPreviousVisibilityBoundary() {
        User user = user(10);
        ChatRoom room = room(1);
        room.updateLastMessage("새 메시지", BASE_TIME.plusHours(2));
        ChatRoomMember member = member(room, user);
        LocalDateTime visibleMessageFrom = BASE_TIME.plusHours(1);
        ReflectionTestUtils.setField(member, "leftAt", visibleMessageFrom);
        ReflectionTestUtils.setField(member, "visibleMessageFrom", visibleMessageFrom);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(member));

        LocalDateTime result = chatDirectRoomAccessService.prepareAccessAndGetVisibleMessageFrom(room, user);

        assertThat(result).isEqualTo(visibleMessageFrom);
        assertThat(member.hasLeft()).isFalse();
    }

    private User user(Integer id) {
        return UserFixture.createUserWithId(
            UniversityFixture.createWithId(1),
            id,
            "사용자" + id,
            "2024" + String.format("%04d", id),
            UserRole.USER
        );
    }

    private ChatRoom room(Integer id) {
        ChatRoom room = ChatRoom.directOf();
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", BASE_TIME);
        return room;
    }

    private ChatRoomMember member(ChatRoom room, User user) {
        ChatRoomMember member = ChatRoomMember.of(room, user, BASE_TIME);
        ReflectionTestUtils.setField(member, "createdAt", BASE_TIME);
        return member;
    }
}
