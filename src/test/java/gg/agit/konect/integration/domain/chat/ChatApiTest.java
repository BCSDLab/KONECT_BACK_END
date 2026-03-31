package gg.agit.konect.integration.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

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
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
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
    private ChatRoomMemberRepository chatRoomMemberRepository;

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
    private University university;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
        clearPersistenceContext();
    }

    @Nested
    @DisplayName("POST /chats/rooms - 일반 채팅방 생성")
    class CreateDirectChatRoom {

        @BeforeEach
        void setUpDirectChatFixture() {
            targetUser = createUser("상대유저", "2021136002");
            clearPersistenceContext();
        }

        @Test
        @DisplayName("일반 채팅방을 생성한다")
        void createDirectChatRoomSuccess() throws Exception {
            // given
            long beforeCount = countDirectRoomsBetween(normalUser, targetUser);
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms", new ChatRoomCreateRequest(targetUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").isNumber());

            clearPersistenceContext();
            assertThat(chatRoomRepository.findByTwoUsers(normalUser.getId(), targetUser.getId())).isPresent();
            assertThat(countDirectRoomsBetween(normalUser, targetUser)).isEqualTo(beforeCount + 1);
        }

        @Test
        @DisplayName("자기 자신과 채팅방을 만들면 400을 반환한다")
        void createDirectChatRoomWithSelfFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms", new ChatRoomCreateRequest(normalUser.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_CREATE_CHAT_ROOM_WITH_SELF"));
        }

        @Test
        @DisplayName("존재하지 않는 대상 유저면 404를 반환한다")
        void createDirectChatRoomWithMissingUserFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms", new ChatRoomCreateRequest(99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_USER"));
        }

        @Test
        @DisplayName("이미 같은 상대와 채팅방이 있으면 기존 방을 반환한다")
        void createDirectChatRoomReturnsExistingRoom() throws Exception {
            // given
            ChatRoom existingRoom = createDirectChatRoom(normalUser, targetUser);
            long beforeCount = countDirectRoomsBetween(normalUser, targetUser);
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms", new ChatRoomCreateRequest(targetUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(existingRoom.getId()));

            clearPersistenceContext();
            assertThat(chatRoomRepository.findByTwoUsers(normalUser.getId(), targetUser.getId()))
                .isPresent()
                .get()
                .extracting(ChatRoom::getId)
                .isEqualTo(existingRoom.getId());
            assertThat(countDirectRoomsBetween(normalUser, targetUser)).isEqualTo(beforeCount);
            assertThat(chatRoomMemberRepository.findByChatRoomId(existingRoom.getId())).hasSize(2);
        }
    }

    @Nested
    @DisplayName("POST /chats/rooms/admin, GET /chats/rooms - 관리자 전용 방 생성 및 조회")
    class AdminChatRoom {

        @BeforeEach
        void setUpAdminChatFixture() {
            adminUser = persist(UserFixture.createAdmin(university));
            clearPersistenceContext();
        }

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

        @BeforeEach
        void setUpMessageFixture() {
            targetUser = createUser("상대유저", "2021136002");
            clearPersistenceContext();
        }

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

        @Test
        @DisplayName("빈 메시지를 전송하면 400을 반환한다")
        void sendBlankMessageFails() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest(" "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
        }

        @Test
        @DisplayName("1000자를 초과한 메시지를 전송하면 400을 반환한다")
        void sendTooLongMessageFails() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(normalUser.getId());
            String tooLongContent = "a".repeat(1001);

            // when & then
            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest(tooLongContent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
        }
    }

    @Nested
    @DisplayName("GET /chats/rooms/{chatRoomId} - 채팅방 메시지 조회 실패")
    class GetMessagesFail {

        @BeforeEach
        void setUpMessageAccessFixture() {
            targetUser = createUser("상대유저", "2021136002");
            outsiderUser = createUser("외부유저", "2021136003");
            clearPersistenceContext();
        }

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
    @DisplayName("GET /chats/rooms/search - 채팅 검색")
    class SearchChats {

        private User secondTargetUser;
        private Club developmentClub;

        @BeforeEach
        void setUpSearchFixture() {
            targetUser = createUser("개발팀", "2021136002");
            secondTargetUser = createUser("개발자", "2021136003");
            outsiderUser = createUser("외부유저", "2021136004");
            developmentClub = persist(ClubFixture.create(university, "개발동아리"));
            createClubMember(developmentClub, normalUser);
            clearPersistenceContext();
        }

        @Test
        @DisplayName("채팅방 이름과 상대방 이름으로 검색 결과를 분리해서 반환한다")
        void searchChatsReturnsRoomMatchesForDirectAndGroupRooms() throws Exception {
            // given
            ChatRoom directRoom = createDirectChatRoom(normalUser, targetUser);
            persistChatMessage(directRoom, normalUser, "안녕하세요");
            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/chats/rooms/search?keyword=개발&page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMatches.totalCount").value(2))
                .andExpect(jsonPath("$.roomMatches.currentCount").value(2))
                .andExpect(jsonPath("$.roomMatches.totalPage").value(1))
                .andExpect(jsonPath("$.roomMatches.currentPage").value(1))
                .andExpect(jsonPath("$.roomMatches.rooms[0].roomName").value("개발팀"))
                .andExpect(jsonPath("$.roomMatches.rooms[0].chatType").value("DIRECT"))
                .andExpect(jsonPath("$.roomMatches.rooms[1].roomName").value("개발동아리"))
                .andExpect(jsonPath("$.roomMatches.rooms[1].chatType").value("GROUP"))
                .andExpect(jsonPath("$.messageMatches.totalCount").value(0))
                .andExpect(jsonPath("$.messageMatches.currentCount").value(0));
        }

        @Test
        @DisplayName("메시지 검색은 접근 가능한 방에서 방별 최신 매칭 메시지 1개만 반환한다")
        void searchChatsReturnsLatestMatchingMessagePerAccessibleRoom() throws Exception {
            // given
            ChatRoom firstRoom = createDirectChatRoom(normalUser, targetUser);
            ChatRoom secondRoom = createDirectChatRoom(normalUser, secondTargetUser);
            ChatRoom outsiderRoom = createDirectChatRoom(outsiderUser, targetUser);

            persistChatMessage(firstRoom, normalUser, "첫 번째 키워드");
            persistChatMessage(secondRoom, secondTargetUser, "두 번째 키워드");
            persistChatMessage(outsiderRoom, outsiderUser, "외부 키워드");
            persistChatMessage(firstRoom, targetUser, "최신 키워드");
            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/chats/rooms/search?keyword=키워드&page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMatches.totalCount").value(0))
                .andExpect(jsonPath("$.messageMatches.totalCount").value(2))
                .andExpect(jsonPath("$.messageMatches.currentCount").value(2))
                .andExpect(jsonPath("$.messageMatches.messages[0].roomName").value("개발팀"))
                .andExpect(jsonPath("$.messageMatches.messages[0].matchedMessage").value("최신 키워드"))
                .andExpect(jsonPath("$.messageMatches.messages[1].roomName").value("개발자"))
                .andExpect(jsonPath("$.messageMatches.messages[1].matchedMessage").value("두 번째 키워드"));
        }

        @Test
        @DisplayName("채팅방 검색 결과에 페이지네이션을 적용한다")
        void searchChatsAppliesPaginationToRoomMatches() throws Exception {
            // given
            createDirectChatRoom(normalUser, targetUser);
            createDirectChatRoom(normalUser, secondTargetUser);
            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/chats/rooms/search?keyword=개발&page=2&limit=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMatches.totalCount").value(3))
                .andExpect(jsonPath("$.roomMatches.currentCount").value(1))
                .andExpect(jsonPath("$.roomMatches.totalPage").value(3))
                .andExpect(jsonPath("$.roomMatches.currentPage").value(2))
                .andExpect(jsonPath("$.roomMatches.rooms[0].roomName").value("개발자"));
        }
    }

    @Nested
    @DisplayName("POST /chats/rooms/{chatRoomId}/mute - 채팅방 뮤트 토글")
    class ToggleMute {

        @BeforeEach
        void setUpMuteFixture() {
            targetUser = createUser("상대유저", "2021136002");
            clearPersistenceContext();
        }

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
            )).matches(setting -> setting.isEmpty() || !setting.get().getIsMuted());
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

    private User createUser(String name, String studentId) {
        return persist(UserFixture.createUser(university, name, studentId));
    }

    private ClubMember createClubMember(Club club, User user) {
        Club managedClub = entityManager.getReference(Club.class, club.getId());
        User managedUser = entityManager.getReference(User.class, user.getId());
        ClubMember clubMember = persist(ClubMemberFixture.createMember(managedClub, managedUser));
        clearPersistenceContext();
        return clubMember;
    }

    private ChatMessage persistChatMessage(ChatRoom chatRoom, User sender, String content) {
        ChatRoom managedChatRoom = entityManager.getReference(ChatRoom.class, chatRoom.getId());
        User managedSender = entityManager.getReference(User.class, sender.getId());

        ChatMessage chatMessage = persist(ChatMessage.of(managedChatRoom, managedSender, content));
        managedChatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());
        entityManager.flush();
        clearPersistenceContext();
        return chatMessage;
    }

    private long countDirectRoomsBetween(User firstUser, User secondUser) {
        return chatRoomRepository.findByUserId(firstUser.getId()).stream()
            .map(ChatRoom::getId)
            .filter(roomId -> isDirectRoomBetween(roomId, firstUser.getId(), secondUser.getId()))
            .count();
    }

    private boolean isDirectRoomBetween(Integer roomId, Integer firstUserId, Integer secondUserId) {
        List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findByChatRoomId(roomId);
        return roomMembers.size() == 2
            && roomMembers.stream().anyMatch(member -> member.getUserId().equals(firstUserId))
            && roomMembers.stream().anyMatch(member -> member.getUserId().equals(secondUserId));
    }
}
