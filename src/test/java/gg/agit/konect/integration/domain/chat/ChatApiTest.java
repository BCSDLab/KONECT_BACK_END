package gg.agit.konect.integration.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatApiTest extends IntegrationTestSupport {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private NotificationMuteSettingRepository notificationMuteSettingRepository;

    @MockitoBean
    private ChatPresenceService chatPresenceService;

    @MockitoBean
    private NotificationService notificationService;

    private User adminUser;
    private User normalUser;
    private User targetUser;
    private User outsiderUser;
    private Club club;

    @BeforeEach
    void setUp() {
        University university = persist(UniversityFixture.create());
        adminUser = persist(UserFixture.createAdmin(university));
        normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
        targetUser = persist(UserFixture.createUser(university, "상대유저", "2021136002"));
        outsiderUser = persist(UserFixture.createUser(university, "외부유저", "2021136003"));
        club = persist(ClubFixture.create(university, "BCSD Lab"));
        persist(ClubMemberFixture.createPresident(club, normalUser));
        persist(ClubMemberFixture.createMember(club, targetUser));
        clearPersistenceContext();
    }

    @Nested
    @DisplayName("POST /chats/rooms - 일반 채팅방 생성")
    class CreateDirectChatRoom {

        @Test
        @DisplayName("일반 채팅방을 생성한다")
        void createDirectChatRoomSuccess() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms", new ChatRoomCreateRequest(targetUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").isNumber());

            clearPersistenceContext();
            assertThat(chatRoomRepository.findByTwoUsers(normalUser.getId(), targetUser.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("POST /chats/rooms/admin, GET /chats/rooms - 관리자 전용 방 생성 및 조회")
    class AdminChatRoom {

        @Test
        @DisplayName("관리자 전용 방을 만들고 목록에서 조회한다")
        void createAdminChatRoomAndGetRoomsSuccess() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when
            performPost("/chats/rooms/admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").isNumber());

            // then
            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].chatType").value("DIRECT"))
                .andExpect(jsonPath("$.rooms[0].roomName").value(adminUser.getName()))
                .andExpect(jsonPath("$.rooms[0].lastMessage").doesNotExist())
                .andExpect(jsonPath("$.rooms[0].isMuted").value(false));
        }
    }

    @Nested
    @DisplayName("POST /chats/rooms/{chatRoomId}/messages - 메시지 전송")
    class SendMessage {

        @Test
        @DisplayName("메시지를 전송하고 응답 형태를 반환한다")
        void sendMessageSuccess() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest("안녕하세요"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").isNumber())
                .andExpect(jsonPath("$.senderId").value(normalUser.getId()))
                .andExpect(jsonPath("$.senderName").doesNotExist())
                .andExpect(jsonPath("$.content").value("안녕하세요"))
                .andExpect(jsonPath("$.isRead").value(true))
                .andExpect(jsonPath("$.unreadCount").isNumber())
                .andExpect(jsonPath("$.isMine").value(true));

            clearPersistenceContext();
            assertThat(chatMessageRepository.findByChatRoomId(chatRoom.getId(), PageRequest.of(0, 20)).getContent())
                .hasSize(1)
                .extracting(ChatMessage::getContent)
                .containsExactly("안녕하세요");
        }
    }

    @Nested
    @DisplayName("GET /chats/rooms/{chatRoomId} - 채팅방 메시지 조회 실패")
    class GetMessagesFail {

        @Test
        @DisplayName("존재하지 않는 채팅방이면 404를 반환한다")
        void getMessagesNotFound() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/chats/rooms/99999?page=1&limit=20")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_CHAT_ROOM"));
        }

        @Test
        @DisplayName("참여하지 않은 사용자가 조회하면 403을 반환한다")
        void getMessagesForbidden() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(outsiderUser.getId());

            // when & then
            performGet("/chats/rooms/" + chatRoom.getId() + "?page=1&limit=20")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }
    }

    @Nested
    @DisplayName("POST /chats/rooms/{chatRoomId}/mute - 채팅방 뮤트 토글")
    class ToggleMute {

        @Test
        @DisplayName("뮤트를 켰다가 다시 끈다")
        void toggleMuteSuccessAndDuplicateProcessing() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms/" + chatRoom.getId() + "/mute")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMuted").value(true));

            performPost("/chats/rooms/" + chatRoom.getId() + "/mute")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMuted").value(false));

            clearPersistenceContext();
            assertThat(notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(
                NotificationTargetType.CHAT_ROOM,
                chatRoom.getId(),
                normalUser.getId()
            )).isPresent()
                .get()
                .extracting(setting -> setting.getIsMuted())
                .isEqualTo(false);
        }
    }

    private ChatRoom createDirectChatRoom(User firstUser, User secondUser) {
        ChatRoom chatRoom = persist(ChatRoom.directOf());
        LocalDateTime joinedAt = chatRoom.getCreatedAt();
        ChatRoom managedChatRoom = entityManager.getReference(ChatRoom.class, chatRoom.getId());
        User managedFirstUser = entityManager.getReference(User.class, firstUser.getId());
        User managedSecondUser = entityManager.getReference(User.class, secondUser.getId());

        persist(ChatRoomMember.of(managedChatRoom, managedFirstUser, joinedAt));
        persist(ChatRoomMember.of(managedChatRoom, managedSecondUser, joinedAt));
        clearPersistenceContext();
        return chatRoom;
    }
}
