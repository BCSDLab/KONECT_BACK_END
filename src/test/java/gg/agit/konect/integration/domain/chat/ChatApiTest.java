package gg.agit.konect.integration.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomNameUpdateRequest;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

import org.springframework.util.LinkedMultiValueMap;

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
            assertThat(chatRoomRepository.findByTwoUsers(normalUser.getId(), targetUser.getId(), ChatType.DIRECT))
                .isPresent();
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
            assertThat(chatRoomRepository.findByTwoUsers(normalUser.getId(), targetUser.getId(), ChatType.DIRECT))
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
    @DisplayName("GET /chats/rooms/invitables - 초대 가능 사용자 조회")
    class GetInvitableUsers {

        private User bcsdUser;
        private User cseUser;
        private User directOnlyUser;
        private User multiClubUser;
        private User withdrawnUser;
        private User adminCandidate;

        @BeforeEach
        void setUpInvitableUsersFixture() {
            bcsdUser = createUser("김비씨", "2021136002");
            cseUser = createUser("이씨에스", "2021136003");
            directOnlyUser = createUser("박다이렉트", "2021136004");
            multiClubUser = createUser("정멀티", "2021136006");
            withdrawnUser = createUser("탈퇴예정", "2021136005");
            adminCandidate = persist(UserFixture.createAdmin(university));

            Club bcsd = persist(ClubFixture.create(university, "BCSD"));
            Club cse = persist(ClubFixture.create(university, "CSE&Biz"));

            User managedNormalUser = entityManager.getReference(User.class, normalUser.getId());
            User managedBcsdUser = entityManager.getReference(User.class, bcsdUser.getId());
            User managedCseUser = entityManager.getReference(User.class, cseUser.getId());
            User managedMultiClubUser = entityManager.getReference(User.class, multiClubUser.getId());
            Club managedBcsd = entityManager.getReference(Club.class, bcsd.getId());
            Club managedCse = entityManager.getReference(Club.class, cse.getId());

            persist(ClubMember.builder()
                .club(managedBcsd)
                .user(managedNormalUser)
                .clubPosition(ClubPosition.MEMBER)
                .build());
            persist(ClubMember.builder()
                .club(managedBcsd)
                .user(managedBcsdUser)
                .clubPosition(ClubPosition.MEMBER)
                .build());
            persist(ClubMember.builder()
                .club(managedBcsd)
                .user(managedMultiClubUser)
                .clubPosition(ClubPosition.MEMBER)
                .build());
            persist(ClubMember.builder()
                .club(managedCse)
                .user(managedNormalUser)
                .clubPosition(ClubPosition.MEMBER)
                .build());
            persist(ClubMember.builder()
                .club(managedCse)
                .user(managedCseUser)
                .clubPosition(ClubPosition.MEMBER)
                .build());
            persist(ClubMember.builder()
                .club(managedCse)
                .user(managedMultiClubUser)
                .clubPosition(ClubPosition.MEMBER)
                .build());

            ChatRoom bcsdRoom = persist(ChatRoom.groupOf(bcsd));
            ChatRoom cseRoom = persist(ChatRoom.groupOf(cse));
            createDirectChatRoom(normalUser, directOnlyUser);
            createDirectChatRoom(normalUser, adminCandidate);

            addRoomMember(bcsdRoom, normalUser);
            addRoomMember(bcsdRoom, bcsdUser);
            addRoomMember(bcsdRoom, multiClubUser);
            addRoomMember(cseRoom, normalUser);
            addRoomMember(cseRoom, cseUser);
            addRoomMember(cseRoom, multiClubUser);
            addRoomMember(cseRoom, withdrawnUser);

            entityManager.getReference(User.class, withdrawnUser.getId()).withdraw(LocalDateTime.now());

            clearPersistenceContext();
        }

        @Test
        @DisplayName("기본 정렬은 동아리 섹션과 기타 섹션으로 반환한다")
        void getInvitableUsersGroupedByClub() throws Exception {
            mockLoginUser(normalUser.getId());

            performGet("/chats/rooms/invitables")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(4))
                .andExpect(jsonPath("$.currentCount").value(4))
                .andExpect(jsonPath("$.totalPage").value(1))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.sortBy").value("CLUB"))
                .andExpect(jsonPath("$.grouped").value(true))
                .andExpect(jsonPath("$.users").isEmpty())
                .andExpect(jsonPath("$.sections[0].clubName").value("BCSD"))
                .andExpect(jsonPath("$.sections[0].users[0].name").value("김비씨"))
                .andExpect(jsonPath("$.sections[0].users[1].name").value("정멀티"))
                .andExpect(jsonPath("$.sections[1].clubName").value("CSE&Biz"))
                .andExpect(jsonPath("$.sections[1].users[0].name").value("이씨에스"))
                .andExpect(jsonPath("$.sections[1].users[1]").doesNotExist())
                .andExpect(jsonPath("$.sections[2].clubName").value("기타"))
                .andExpect(jsonPath("$.sections[2].users[0].name").value("박다이렉트"))
                .andExpect(jsonPath("$.sections[2].users[1]").doesNotExist())
                .andExpect(jsonPath("$.sections[*].users[*].name").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.hasItem("탈퇴예정")
                )))
                .andExpect(jsonPath("$.sections[*].users[*].name").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.hasItem("관리자")
                )));
        }

        @Test
        @DisplayName("이름 정렬이면 섹션 없이 단일 리스트로 반환하고 검색이 적용된다")
        void getInvitableUsersSortedByName() throws Exception {
            mockLoginUser(normalUser.getId());
            LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>(Map.of(
                "sortBy", List.of("NAME"),
                "query", List.of("2021136004")
            ));

            performGet("/chats/rooms/invitables", params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.currentCount").value(1))
                .andExpect(jsonPath("$.totalPage").value(1))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.sortBy").value("NAME"))
                .andExpect(jsonPath("$.grouped").value(false))
                .andExpect(jsonPath("$.sections").isEmpty())
                .andExpect(jsonPath("$.users[0].name").value("박다이렉트"))
                .andExpect(jsonPath("$.users[1]").doesNotExist())
                .andExpect(jsonPath("$.users[*].name").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.hasItem("탈퇴예정")
                )))
                .andExpect(jsonPath("$.users[*].name").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.hasItem("관리자")
                )));
        }

        @Test
        @DisplayName("페이지네이션을 적용하면 현재 페이지 사용자만 반환한다")
        void getInvitableUsersWithPagination() throws Exception {
            mockLoginUser(normalUser.getId());
            LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>(Map.of(
                "sortBy", List.of("NAME"),
                "page", List.of("2"),
                "limit", List.of("2")
            ));

            performGet("/chats/rooms/invitables", params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(4))
                .andExpect(jsonPath("$.currentCount").value(2))
                .andExpect(jsonPath("$.totalPage").value(2))
                .andExpect(jsonPath("$.currentPage").value(2))
                .andExpect(jsonPath("$.users[0].name").value("이씨에스"))
                .andExpect(jsonPath("$.users[1].name").value("정멀티"))
                .andExpect(jsonPath("$.users[2]").doesNotExist());
        }

        @Test
        @DisplayName("동아리 정렬은 페이지 경계가 동아리를 가로질러도 섹션 헤더를 유지한다")
        void getInvitableUsersWithClubPaginationAcrossSections() throws Exception {
            mockLoginUser(normalUser.getId());

            createGroupedInviteCandidates("분할A", "분할A", 10);
            createGroupedInviteCandidates("분할B", "분할B", 20);
            createGroupedInviteCandidates("분할C", "분할C", 30);
            clearPersistenceContext();

            LinkedMultiValueMap<String, String> firstPageParams = new LinkedMultiValueMap<>(Map.of(
                "sortBy", List.of("CLUB"),
                "query", List.of("분할"),
                "page", List.of("1"),
                "limit", List.of("20")
            ));

            performGet("/chats/rooms/invitables", firstPageParams)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(60))
                .andExpect(jsonPath("$.currentCount").value(20))
                .andExpect(jsonPath("$.totalPage").value(3))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.sections[0].clubName").value("분할A"))
                .andExpect(jsonPath("$.sections[0].users.length()").value(10))
                .andExpect(jsonPath("$.sections[1].clubName").value("분할B"))
                .andExpect(jsonPath("$.sections[1].users.length()").value(10))
                .andExpect(jsonPath("$.sections[2]").doesNotExist());

            LinkedMultiValueMap<String, String> secondPageParams = new LinkedMultiValueMap<>(Map.of(
                "sortBy", List.of("CLUB"),
                "query", List.of("분할"),
                "page", List.of("2"),
                "limit", List.of("20")
            ));

            performGet("/chats/rooms/invitables", secondPageParams)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(60))
                .andExpect(jsonPath("$.currentCount").value(20))
                .andExpect(jsonPath("$.totalPage").value(3))
                .andExpect(jsonPath("$.currentPage").value(2))
                .andExpect(jsonPath("$.sections[0].clubName").value("분할B"))
                .andExpect(jsonPath("$.sections[0].users.length()").value(10))
                .andExpect(jsonPath("$.sections[0].users[0].name").value("분할B11"))
                .andExpect(jsonPath("$.sections[0].users[9].name").value("분할B20"))
                .andExpect(jsonPath("$.sections[1].clubName").value("분할C"))
                .andExpect(jsonPath("$.sections[1].users.length()").value(10))
                .andExpect(jsonPath("$.sections[1].users[0].name").value("분할C01"))
                .andExpect(jsonPath("$.sections[1].users[9].name").value("분할C10"))
                .andExpect(jsonPath("$.sections[2]").doesNotExist());
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
            assertThat(
                chatMessageRepository.findByChatRoomId(chatRoom.getId(), null, PageRequest.of(0, 20)).getContent()
            )
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
    @DisplayName("PATCH /chats/rooms/{chatRoomId}/name - 채팅방 이름 수정")
    class UpdateChatRoomName {

        @BeforeEach
        void setUpRoomNameFixture() {
            targetUser = createUser("상대유저", "2021136002");
            outsiderUser = createUser("외부유저", "2021136003");
            clearPersistenceContext();
        }

        @Test
        @DisplayName("내가 수정한 채팅방 이름은 내 목록에만 반영된다")
        void updateChatRoomNameOnlyForCurrentUser() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);

            mockLoginUser(normalUser.getId());
            performPatch("/chats/rooms/" + chatRoom.getId() + "/name", new ChatRoomNameUpdateRequest("알바 이야기방"))
                .andExpect(status().isOk());

            clearPersistenceContext();
            assertThat(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), normalUser.getId()))
                .isPresent()
                .get()
                .extracting(ChatRoomMember::getCustomRoomName)
                .isEqualTo("알바 이야기방");

            // when & then
            mockLoginUser(normalUser.getId());
            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].roomName").value("알바 이야기방"));

            mockLoginUser(targetUser.getId());
            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].roomName").value(normalUser.getName()));
        }

        @Test
        @DisplayName("공백 이름으로 요청하면 기본 채팅방 이름으로 되돌린다")
        void blankRoomNameResetsToDefault() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(normalUser.getId());

            performPatch("/chats/rooms/" + chatRoom.getId() + "/name", new ChatRoomNameUpdateRequest("알바 이야기방"))
                .andExpect(status().isOk());

            // when
            performPatch("/chats/rooms/" + chatRoom.getId() + "/name", new ChatRoomNameUpdateRequest("   "))
                .andExpect(status().isOk());

            // then
            clearPersistenceContext();
            assertThat(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), normalUser.getId()))
                .isPresent()
                .get()
                .extracting(ChatRoomMember::getCustomRoomName)
                .isNull();

            mockLoginUser(normalUser.getId());
            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].roomName").value(targetUser.getName()));
        }

        @Test
        @DisplayName("참여하지 않은 사용자는 채팅방 이름을 수정할 수 없다")
        void updateChatRoomNameForbidden() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(outsiderUser.getId());

            // when & then
            performPatch("/chats/rooms/" + chatRoom.getId() + "/name", new ChatRoomNameUpdateRequest("몰래 바꾸기"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }
    }

    @Nested
    @DisplayName("DELETE /chats/rooms/{chatRoomId} - 채팅방 나가기")
    class LeaveChatRoom {

        @BeforeEach
        void setUpLeaveFixture() {
            targetUser = createUser("상대유저", "2021136002");
            clearPersistenceContext();
        }

        @Test
        @DisplayName("1:1 채팅방을 나가면 목록에서 숨겨지고 새 메시지부터 다시 보인다")
        void leaveDirectChatRoomAndShowOnlyNewMessages() throws Exception {
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);

            mockLoginUser(normalUser.getId());
            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest("첫 메시지"))
                .andExpect(status().isOk());

            performDelete("/chats/rooms/" + chatRoom.getId())
                .andExpect(status().isNoContent());

            clearPersistenceContext();
            ChatRoomMember leftMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), normalUser.getId())
                .orElseThrow();
            assertThat(leftMember.hasLeft()).isTrue();

            mockLoginUser(normalUser.getId());
            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isEmpty());

            performGet("/chats/rooms/" + chatRoom.getId() + "?page=1&limit=20")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));

            mockLoginUser(targetUser.getId());
            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest("다시 안녕"))
                .andExpect(status().isOk());

            mockLoginUser(normalUser.getId());
            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].roomId").value(chatRoom.getId()))
                .andExpect(jsonPath("$.rooms[0].lastMessage").value("다시 안녕"))
                .andExpect(jsonPath("$.rooms[0].unreadCount").value(1));

            performGet("/chats/rooms/" + chatRoom.getId() + "?page=1&limit=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.messages[0].content").value("다시 안녕"));

            mockLoginUser(targetUser.getId());
            performGet("/chats/rooms/" + chatRoom.getId() + "?page=1&limit=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.messages[0].content").value("다시 안녕"))
                .andExpect(jsonPath("$.messages[1].content").value("첫 메시지"));
        }

        @Test
        @DisplayName("나간 뒤 다시 채팅방을 열면 처음 대화하는 것처럼 빈 메시지 목록을 본다")
        void createOrGetChatRoomAfterLeaveStartsFresh() throws Exception {
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);

            mockLoginUser(normalUser.getId());
            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest("첫 메시지"))
                .andExpect(status().isOk());

            performDelete("/chats/rooms/" + chatRoom.getId())
                .andExpect(status().isNoContent());

            performPost("/chats/rooms", new ChatRoomCreateRequest(targetUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(chatRoom.getId()));

            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].roomId").value(chatRoom.getId()))
                .andExpect(jsonPath("$.rooms[0].lastMessage").doesNotExist());

            performGet("/chats/rooms/" + chatRoom.getId() + "?page=1&limit=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.messages").isEmpty());
        }

        @Test
        @DisplayName("나간 뒤 새 메시지가 오기 전에는 방 조작이 불가능하다")
        void cannotOperateHiddenDirectRoomBeforeNewMessage() throws Exception {
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);

            mockLoginUser(normalUser.getId());
            performDelete("/chats/rooms/" + chatRoom.getId())
                .andExpect(status().isNoContent());

            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest("몰래 보내기"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));

            performPatch("/chats/rooms/" + chatRoom.getId() + "/name", new ChatRoomNameUpdateRequest("숨김방"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));

            performPost("/chats/rooms/" + chatRoom.getId() + "/mute")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("동아리 채팅방은 나갈 수 없다")
        void leaveGroupChatRoomFails() throws Exception {
            Club club = persist(ClubFixture.create(university));
            ChatRoom groupRoom = persist(ChatRoom.groupOf(club));
            ChatRoom managedGroupRoom = entityManager.getReference(ChatRoom.class, groupRoom.getId());
            User managedNormalUser = entityManager.getReference(User.class, normalUser.getId());
            persist(ChatRoomMember.of(managedGroupRoom, managedNormalUser, groupRoom.getCreatedAt()));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            performDelete("/chats/rooms/" + groupRoom.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_LEAVE_GROUP_CHAT_ROOM"));
        }

        @Test
        @DisplayName("일반 그룹 채팅방은 멤버십 삭제 방식으로 나갈 수 있다")
        void leaveOpenGroupChatRoomDeletesMembership() throws Exception {
            ChatRoom openGroupRoom = persist(ChatRoom.groupOf());
            ChatRoom managedOpenGroupRoom = entityManager.getReference(ChatRoom.class, openGroupRoom.getId());
            User managedNormalUser = entityManager.getReference(User.class, normalUser.getId());
            User managedTargetUser = entityManager.getReference(User.class, targetUser.getId());
            persist(ChatRoomMember.of(managedOpenGroupRoom, managedNormalUser, openGroupRoom.getCreatedAt()));
            persist(ChatRoomMember.of(managedOpenGroupRoom, managedTargetUser, openGroupRoom.getCreatedAt()));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());

            performDelete("/chats/rooms/" + openGroupRoom.getId())
                .andExpect(status().isNoContent());

            clearPersistenceContext();
            assertThat(chatRoomMemberRepository.findByChatRoomIdAndUserId(openGroupRoom.getId(), normalUser.getId()))
                .isEmpty();
            assertThat(chatRoomMemberRepository.findByChatRoomIdAndUserId(openGroupRoom.getId(), targetUser.getId()))
                .isPresent();
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
            ChatRoom groupRoom = persist(ChatRoom.groupOf(developmentClub));
            addRoomMember(groupRoom, normalUser);
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
        @DisplayName("나간 1:1 채팅방의 숨김 메시지는 검색 결과에 노출되지 않는다")
        void searchChatsExcludesHiddenMessagesFromLeftDirectRoom() throws Exception {
            // given
            ChatRoom directRoom = createDirectChatRoom(normalUser, targetUser);

            mockLoginUser(normalUser.getId());
            performPost("/chats/rooms/" + directRoom.getId() + "/messages", new ChatMessageSendRequest("비밀 키워드"))
                .andExpect(status().isOk());
            performDelete("/chats/rooms/" + directRoom.getId())
                .andExpect(status().isNoContent());

            mockLoginUser(targetUser.getId());
            performPost("/chats/rooms/" + directRoom.getId() + "/messages", new ChatMessageSendRequest("다시 안녕"))
                .andExpect(status().isOk());

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/chats/rooms/search?keyword=비밀&page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMatches.totalCount").value(0))
                .andExpect(jsonPath("$.messageMatches.totalCount").value(0))
                .andExpect(jsonPath("$.messageMatches.currentCount").value(0));
        }

        @Test
        @DisplayName("메시지 검색은 LIKE 특수문자를 리터럴로 처리한다")
        void searchChatsTreatsLikeSpecialCharactersAsLiteral() throws Exception {
            // given
            ChatRoom firstRoom = createDirectChatRoom(normalUser, targetUser);
            ChatRoom secondRoom = createDirectChatRoom(normalUser, secondTargetUser);

            persistChatMessage(firstRoom, normalUser, "100% 완료");
            persistChatMessage(secondRoom, secondTargetUser, "1000 완료");
            mockLoginUser(normalUser.getId());
            LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>(Map.of(
                "keyword", List.of("100%"),
                "page", List.of("1"),
                "limit", List.of("10")
            ));

            // when & then
            performGet("/chats/rooms/search", params)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMatches.totalCount").value(0))
                .andExpect(jsonPath("$.messageMatches.totalCount").value(1))
                .andExpect(jsonPath("$.messageMatches.currentCount").value(1))
                .andExpect(jsonPath("$.messageMatches.messages[0].roomName").value("개발팀"))
                .andExpect(jsonPath("$.messageMatches.messages[0].matchedMessage").value("100% 완료"));
        }

        @Test
        @DisplayName("커스텀 채팅방 이름이 있어도 기본 상대방 이름으로 검색할 수 있다")
        void searchChatsMatchesDefaultNameEvenWithCustomRoomName() throws Exception {
            // given
            ChatRoom directRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(normalUser.getId());
            performPatch("/chats/rooms/" + directRoom.getId() + "/name", new ChatRoomNameUpdateRequest("내 메모"))
                .andExpect(status().isOk());

            // when & then
            performGet("/chats/rooms/search?keyword=개발팀&page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMatches.totalCount").value(1))
                .andExpect(jsonPath("$.roomMatches.currentCount").value(1))
                .andExpect(jsonPath("$.roomMatches.rooms[0].roomId").value(directRoom.getId()))
                .andExpect(jsonPath("$.roomMatches.rooms[0].roomName").value("내 메모"));
        }

        @Test
        @DisplayName("채팅방 검색 결과에 페이지네이션을 적용한다")
        void searchChatsAppliesPaginationToRoomMatches() throws Exception {
            // given
            createDirectChatRoom(normalUser, targetUser);
            createDirectChatRoom(normalUser, secondTargetUser);
            ChatRoom groupRoom = persist(ChatRoom.groupOf(developmentClub));
            addRoomMember(groupRoom, normalUser);
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

        @Test
        @DisplayName("매우 큰 page 값이어도 빈 검색 결과를 안전하게 반환한다")
        void searchChatsWithVeryLargePageReturnsEmptyResult() throws Exception {
            // given
            createDirectChatRoom(normalUser, targetUser);
            ChatRoom groupRoom = persist(ChatRoom.groupOf(developmentClub));
            addRoomMember(groupRoom, normalUser);
            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/chats/rooms/search?keyword=개발&page=2147483647&limit=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMatches.totalCount").value(2))
                .andExpect(jsonPath("$.roomMatches.currentCount").value(0))
                .andExpect(jsonPath("$.roomMatches.currentPage").value(2147483647));
        }

        @Test
        @DisplayName("limit가 최대값을 초과하면 400을 반환한다")
        void searchChatsWithTooLargeLimitFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/chats/rooms/search?keyword=개발&page=1&limit=101")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("searchChats.limit"));
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
        ClubMember clubMember = persist(ClubMember.builder()
            .club(managedClub)
            .user(managedUser)
            .clubPosition(ClubPosition.MEMBER)
            .build());
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

    private void addRoomMember(ChatRoom chatRoom, User user) {
        ChatRoom managedChatRoom = entityManager.getReference(ChatRoom.class, chatRoom.getId());
        User managedUser = entityManager.getReference(User.class, user.getId());
        persist(ChatRoomMember.of(managedChatRoom, managedUser, chatRoom.getCreatedAt()));
    }

    private void createGroupedInviteCandidates(String clubName, String namePrefix, int count) {
        Club club = persist(ClubFixture.create(university, clubName));
        Club managedClub = entityManager.getReference(Club.class, club.getId());
        User managedNormalUser = entityManager.getReference(User.class, normalUser.getId());

        persist(ClubMember.builder()
            .club(managedClub)
            .user(managedNormalUser)
            .clubPosition(ClubPosition.MEMBER)
            .build());

        ChatRoom groupRoom = persist(ChatRoom.groupOf(club));
        addRoomMember(groupRoom, normalUser);

        for (int index = 1; index <= count; index++) {
            User candidate = createUser(
                String.format("%s%02d", namePrefix, index),
                String.format("202199%04d", index + count * 10)
            );
            User managedCandidate = entityManager.getReference(User.class, candidate.getId());
            persist(ClubMember.builder()
                .club(managedClub)
                .user(managedCandidate)
                .clubPosition(ClubPosition.MEMBER)
                .build());
            addRoomMember(groupRoom, candidate);
        }
    }

    private long countDirectRoomsBetween(User firstUser, User secondUser) {
        return chatRoomRepository.findByUserId(firstUser.getId(), ChatType.DIRECT).stream()
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
