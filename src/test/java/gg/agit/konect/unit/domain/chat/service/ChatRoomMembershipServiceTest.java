package gg.agit.konect.unit.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gg.agit.konect.domain.chat.dto.ChatRoomMemberResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomMembersResponse;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;

@ExtendWith(MockitoExtension.class)
class ChatRoomMembershipServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatRoomMembershipService chatRoomMembershipService;

    @Test
    @DisplayName("채팅방 멤버 목록 조회 성공")
    void getChatRoomMembers_success() {
        // given
        Integer chatRoomId = 1;
        Integer currentUserId = 100;

        User user1 = User.builder()
            .id(100)
            .name("User1")
            .imageUrl("image1.jpg")
            .build();

        User user2 = User.builder()
            .id(200)
            .name("User2")
            .imageUrl("image2.jpg")
            .build();

        ChatRoom chatRoom = ChatRoom.builder()
            .id(chatRoomId)
            .build();

        ChatRoomMember member1 = ChatRoomMember.ofOwner(chatRoom, user1, LocalDateTime.now());
        ChatRoomMember member2 = ChatRoomMember.of(chatRoom, user2, LocalDateTime.now());

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, currentUserId))
            .willReturn(true);
        given(chatRoomMemberRepository.findActiveMembersByChatRoomId(chatRoomId))
            .willReturn(List.of(member1, member2));

        // when
        ChatRoomMembersResponse response = chatRoomMembershipService.getChatRoomMembers(chatRoomId, currentUserId);

        // then
        assertThat(response.members()).hasSize(2);

        ChatRoomMemberResponse firstMember = response.members().get(0);
        assertThat(firstMember.userId()).isEqualTo(100);
        assertThat(firstMember.name()).isEqualTo("User1");
        assertThat(firstMember.isOwner()).isTrue();

        ChatRoomMemberResponse secondMember = response.members().get(1);
        assertThat(secondMember.userId()).isEqualTo(200);
        assertThat(secondMember.name()).isEqualTo("User2");
        assertThat(secondMember.isOwner()).isFalse();
    }

    @Test
    @DisplayName("비멤버가 조회 시도 시 FORBIDDEN 예외 발생")
    void getChatRoomMembers_forbiddenWhenNotMember() {
        // given
        Integer chatRoomId = 1;
        Integer currentUserId = 100;

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, currentUserId))
            .willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatRoomMembershipService.getChatRoomMembers(chatRoomId, currentUserId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    @Test
    @DisplayName("나간 멤버는 조회되지 않음")
    void getChatRoomMembers_excludesLeftMembers() {
        // given
        Integer chatRoomId = 1;
        Integer currentUserId = 100;

        User user1 = User.builder()
            .id(100)
            .name("User1")
            .imageUrl("image1.jpg")
            .build();

        User user2 = User.builder()
            .id(200)
            .name("User2")
            .imageUrl("image2.jpg")
            .build();

        ChatRoom chatRoom = ChatRoom.builder()
            .id(chatRoomId)
            .build();

        ChatRoomMember member1 = ChatRoomMember.of(chatRoom, user1, LocalDateTime.now());
        ChatRoomMember member2 = ChatRoomMember.of(chatRoom, user2, LocalDateTime.now());
        member2.leaveDirectRoom(LocalDateTime.now()); // member2는 나간 상태

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, currentUserId))
            .willReturn(true);
        given(chatRoomMemberRepository.findActiveMembersByChatRoomId(chatRoomId))
            .willReturn(List.of(member1)); // 나간 멤버는 조회되지 않음

        // when
        ChatRoomMembersResponse response = chatRoomMembershipService.getChatRoomMembers(chatRoomId, currentUserId);

        // then
        assertThat(response.members()).hasSize(1);
        assertThat(response.members().get(0).userId()).isEqualTo(100);
    }
}
