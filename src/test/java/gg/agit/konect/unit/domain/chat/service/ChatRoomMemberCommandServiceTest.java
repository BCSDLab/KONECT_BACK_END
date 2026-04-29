package gg.agit.konect.unit.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_INVITE_IN_NON_GROUP_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatRoomMemberCommandService;
import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatRoomMemberCommandServiceTest extends ServiceTestSupport {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomMembershipService chatRoomMembershipService;

    @InjectMocks
    private ChatRoomMemberCommandService chatRoomMemberCommandService;

    @Test
    @DisplayName("초대 요청은 중복 userId와 요청자 자신과 기존 멤버를 제외하고 새 멤버만 추가한다")
    void inviteMembersAddsOnlyNewUsers() {
        // given
        Integer roomId = 10;
        Integer requesterId = 100;
        User requester = createUser(requesterId, "요청자");
        User existingMember = createUser(200, "기존멤버");
        User newMember = createUser(300, "새멤버");
        ChatRoom room = createRoom(roomId, ChatType.GROUP);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(roomId, requesterId)).willReturn(true);
        given(userRepository.findAllByIdIn(List.of(requesterId, existingMember.getId(), newMember.getId())))
            .willReturn(List.of(requester, existingMember, newMember));
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(roomId, existingMember.getId()))
            .willReturn(true);
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(roomId, newMember.getId()))
            .willReturn(false);

        // when
        chatRoomMemberCommandService.inviteMembers(
            requesterId,
            roomId,
            List.of(requesterId, existingMember.getId(), newMember.getId(), newMember.getId())
        );

        // then
        verify(chatRoomMembershipService).ensureMember(any(ChatRoom.class), any(User.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("일반 GROUP이 아니면 초대할 수 없다")
    void inviteMembersToNonGroupRoomFails() {
        // given
        Integer roomId = 10;
        ChatRoom room = createRoom(roomId, ChatType.DIRECT);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));

        // when & then
        assertErrorCode(
            () -> chatRoomMemberCommandService.inviteMembers(100, roomId, List.of(200)),
            CANNOT_INVITE_IN_NON_GROUP_ROOM
        );

        verify(userRepository, never()).findAllByIdIn(any());
    }

    @Test
    @DisplayName("요청자가 active 멤버가 아니면 초대할 수 없다")
    void inviteMembersByInactiveRequesterFails() {
        // given
        Integer roomId = 10;
        Integer requesterId = 100;
        ChatRoom room = createRoom(roomId, ChatType.GROUP);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(roomId, requesterId)).willReturn(false);

        // when & then
        assertErrorCode(
            () -> chatRoomMemberCommandService.inviteMembers(requesterId, roomId, List.of(200)),
            FORBIDDEN_CHAT_ROOM_ACCESS
        );

        verify(userRepository, never()).findAllByIdIn(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 있으면 전체 요청을 실패시킨다")
    void inviteMembersWithMissingUserFails() {
        // given
        Integer roomId = 10;
        Integer requesterId = 100;
        ChatRoom room = createRoom(roomId, ChatType.GROUP);
        User foundUser = createUser(200, "존재사용자");
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(roomId, requesterId)).willReturn(true);
        given(userRepository.findAllByIdIn(List.of(foundUser.getId(), 99999)))
            .willReturn(List.of(foundUser));

        // when & then
        assertErrorCode(
            () -> chatRoomMemberCommandService.inviteMembers(requesterId, roomId, List.of(foundUser.getId(), 99999)),
            NOT_FOUND_USER
        );

        verify(chatRoomMembershipService, never()).ensureMember(any(), any(), any());
    }

    private ChatRoom createRoom(Integer id, ChatType type) {
        ChatRoom room = switch (type) {
            case DIRECT -> ChatRoom.directOf();
            case GROUP -> ChatRoom.groupOf();
            case CLUB_GROUP -> throw new IllegalArgumentException("Use fixture for CLUB_GROUP");
        };
        ReflectionTestUtils.setField(room, "id", id);
        return room;
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

    private void assertErrorCode(ThrowingCallable callable, ApiResponseCode errorCode) {
        assertThatThrownBy(callable)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(errorCode));
    }
}
