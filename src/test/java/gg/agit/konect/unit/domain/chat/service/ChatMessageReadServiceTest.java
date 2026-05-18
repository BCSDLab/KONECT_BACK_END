package gg.agit.konect.unit.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.service.ChatDirectRoomAccessService;
import gg.agit.konect.domain.chat.service.ChatMessageReadService;
import gg.agit.konect.domain.chat.service.ChatRoomSystemAdminService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatMessageReadServiceTest extends ServiceTestSupport {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatRoomSystemAdminService chatRoomSystemAdminService;

    @Mock
    private ChatDirectRoomAccessService chatDirectRoomAccessService;

    private ChatMessageReadService chatMessageReadService;

    @BeforeEach
    void setUp() {
        chatMessageReadService = new ChatMessageReadService(
            chatMessageRepository,
            chatRoomMemberRepository,
            chatRoomSystemAdminService,
            chatDirectRoomAccessService
        );
    }

    @Test
    @DisplayName("어드민은 SYSTEM_ADMIN 문의방에서 다른 어드민 메시지도 내 메시지로 조회한다")
    void getAdminSystemDirectChatRoomMessagesMarksOtherAdminMessagesAsMine() {
        // given
        User viewerAdmin = createUser(99, "viewer admin", UserRole.ADMIN);
        User otherAdmin = createUser(88, "other admin", UserRole.ADMIN);
        User systemAdmin = createUser(SYSTEM_ADMIN_ID, "system admin", UserRole.ADMIN);
        User normalUser = createUser(20, "normal user", UserRole.USER);

        ChatRoom room = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember systemAdminMember = createRoomMember(room, systemAdmin, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember normalUserMember = createRoomMember(room, normalUser, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage otherAdminMessage = createMessage(
            100,
            room,
            otherAdmin,
            "reply",
            LocalDateTime.of(2026, 4, 11, 10, 1)
        );
        PageRequest pageable = PageRequest.of(0, 20);

        given(chatRoomMemberRepository.findByChatRoomId(room.getId()))
            .willReturn(List.of(systemAdminMember, normalUserMember));
        given(chatRoomSystemAdminService.findSystemAdminMember(List.of(systemAdminMember, normalUserMember)))
            .willReturn(systemAdminMember);
        given(chatMessageRepository.findByChatRoomId(eq(room.getId()), nullable(LocalDateTime.class), eq(pageable)))
            .willReturn(new PageImpl<>(List.of(otherAdminMessage), pageable, 1));

        // when
        ChatMessagePageResponse response = chatMessageReadService.getAdminSystemDirectChatRoomMessages(
            viewerAdmin,
            room,
            1,
            20,
            LocalDateTime.of(2026, 4, 11, 10, 2)
        );

        // then
        ChatMessageDetailResponse message = response.messages().getFirst();
        assertThat(message.senderId()).isEqualTo(otherAdmin.getId());
        assertThat(message.isMine()).isTrue();
        assertThat(message.isRead()).isTrue();
    }

    @Test
    @DisplayName("일반 사용자는 direct 방의 어드민 메시지를 상대 메시지로 조회한다")
    void getDirectChatRoomMessagesKeepsAdminMessagesAsOpponentForNormalUser() {
        // given
        User normalUser = createUser(20, "normal user", UserRole.USER);
        User systemAdmin = createUser(SYSTEM_ADMIN_ID, "system admin", UserRole.ADMIN);
        User otherAdmin = createUser(88, "other admin", UserRole.ADMIN);

        ChatRoom room = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember systemAdminMember = createRoomMember(room, systemAdmin, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember normalUserMember = createRoomMember(room, normalUser, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage otherAdminMessage = createMessage(
            100,
            room,
            otherAdmin,
            "reply",
            LocalDateTime.of(2026, 4, 11, 10, 1)
        );
        PageRequest pageable = PageRequest.of(0, 20);

        given(chatRoomMemberRepository.findByChatRoomId(room.getId()))
            .willReturn(List.of(systemAdminMember, normalUserMember));
        given(chatDirectRoomAccessService.prepareAccessAndGetVisibleMessageFrom(room, normalUser))
            .willReturn(null);
        given(chatMessageRepository.findByChatRoomId(eq(room.getId()), nullable(LocalDateTime.class), eq(pageable)))
            .willReturn(new PageImpl<>(List.of(otherAdminMessage), pageable, 1));

        // when
        ChatMessagePageResponse response = chatMessageReadService.getDirectChatRoomMessages(
            normalUser,
            room,
            1,
            20,
            LocalDateTime.of(2026, 4, 11, 10, 2)
        );

        // then
        ChatMessageDetailResponse message = response.messages().getFirst();
        assertThat(message.senderId()).isEqualTo(SYSTEM_ADMIN_ID);
        assertThat(message.isMine()).isFalse();
        assertThat(message.isRead()).isTrue();
    }

    private User createUser(Integer id, String name, UserRole role) {
        return UserFixture.createUserWithId(UniversityFixture.createWithId(1), id, name,
            "2024" + String.format("%04d", id), role);
    }

    private ChatRoom createRoom(Integer id, ChatType type, LocalDateTime createdAt) {
        ChatRoom room = switch (type) {
            case DIRECT -> ChatRoom.directOf();
            case GROUP -> ChatRoom.groupOf();
            case CLUB_GROUP -> ChatRoom.clubGroupOf(ClubFixture.createWithId(UniversityFixture.createWithId(1), 77));
            default -> throw new IllegalArgumentException("Unsupported ChatType: " + type);
        };
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", createdAt);
        return room;
    }

    private ChatRoomMember createRoomMember(ChatRoom room, User user, LocalDateTime lastReadAt) {
        ChatRoomMember member = ChatRoomMember.of(room, user, lastReadAt);
        ReflectionTestUtils.setField(member, "createdAt", lastReadAt);
        return member;
    }

    private ChatMessage createMessage(Integer id, ChatRoom room, User sender, String content, LocalDateTime createdAt) {
        ChatMessage message = ChatMessage.of(room, sender, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        return message;
    }
}
