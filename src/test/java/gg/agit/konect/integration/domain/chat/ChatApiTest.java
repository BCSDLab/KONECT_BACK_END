package gg.agit.konect.integration.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;

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

class ChatApiTest extends IntegrationTestSupport {

    private static final int SYSTEM_ADMIN_ID = 1;
    private static final String CHAT_TEST_DATA_CLEANUP_SQL = """
        DELETE FROM notification_mute_setting;
        DELETE FROM chat_message;
        DELETE FROM chat_room_member;
        DELETE FROM chat_room;
        DELETE FROM club_member;
        DELETE FROM club;
        DELETE FROM users;
        DELETE FROM university;
        """;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private NotificationMuteSettingRepository notificationMuteSettingRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
    public void setUp() {
        transactionTemplate.execute(status -> {
            university = persist(UniversityFixture.create());
            // System Admin을 먼저 생성 - 문의 채팅방용
            adminUser = persist(UserFixture.createAdmin(university));
            // 일부 테스트는 NOT_SUPPORTED로 실행되므로, 공통 픽스처는 명시적 트랜잭션 안에서 만든다.
            if (adminUser.getId() != SYSTEM_ADMIN_ID) {
                entityManager.createNativeQuery("""
                        INSERT INTO users (id, email, name, student_number, role, is_marketing_agreement, image_url, university_id, created_at, updated_at)
                        SELECT ?, 'system@koreatech.ac.kr', '시스템관리자', '2021000001', 'ADMIN', true, 'https://example.com/system-admin.png', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = ?)
                        """
                    ).setParameter(1, SYSTEM_ADMIN_ID)
                    .setParameter(2, university.getId())
                    .setParameter(3, SYSTEM_ADMIN_ID)
                    .executeUpdate();
                entityManager.flush();
            }
            normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
            clearPersistenceContext();
            return null;
        });
    }

    @Transactional
    public ChatRoom createDirectChatRoom(User firstUser, User secondUser) {
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

    private ChatRoom createGroupChatRoomWithOwner(User owner, User... members) {
        ChatRoom groupRoom = persist(ChatRoom.groupOf());
        ChatRoom managedRoom = entityManager.getReference(ChatRoom.class, groupRoom.getId());
        User managedOwner = entityManager.getReference(User.class, owner.getId());
        persist(ChatRoomMember.ofOwner(managedRoom, managedOwner, groupRoom.getCreatedAt()));
        for (User member : members) {
            User managedMember = entityManager.getReference(User.class, member.getId());
            persist(ChatRoomMember.of(managedRoom, managedMember, groupRoom.getCreatedAt()));
        }
        clearPersistenceContext();
        return groupRoom;
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

        ChatRoom groupRoom = persist(ChatRoom.clubGroupOf(club));
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
            // System Admin(ID=1)은 이미 setUp()에서 생성됨
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

            // then - 일반 사용자 관점에서 채팅방이 목록에 보임
            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].chatType").value("DIRECT"))
                .andExpect(jsonPath("$.rooms[0].roomName").exists())
                .andExpect(jsonPath("$.rooms[0].lastMessage").doesNotExist())
                .andExpect(jsonPath("$.rooms[0].isMuted").value(false));
        }

        @Test
        @DisplayName("관리자가 문의방을 다시 열어도 관리자 멤버는 추가되지 않는다")
        void adminCreateOrGetInquiryRoomDoesNotAddAdminMember() throws Exception {
            User anotherAdmin = persist(UserFixture.createAdmin(university));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());
            var createResult = performPost("/chats/rooms/admin")
                .andExpect(status().isOk())
                .andReturn();

            int chatRoomId = parseChatRoomId(createResult);

            mockLoginUser(anotherAdmin.getId());
            performPost("/chats/rooms", new ChatRoomCreateRequest(normalUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId));

            clearPersistenceContext();

            assertThat(chatRoomRepository.findByTwoUsers(SYSTEM_ADMIN_ID, normalUser.getId(), ChatType.DIRECT))
                .isPresent()
                .get()
                .extracting(ChatRoom::getId)
                .isEqualTo(chatRoomId);
            assertThat(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .extracting(ChatRoomMember::getUserId)
                .containsExactlyInAnyOrder(SYSTEM_ADMIN_ID, normalUser.getId());
        }

        @Test
        @DisplayName("관리자는 멤버가 아니어도 문의방 메시지를 조회할 수 있다")
        void adminCanReadInquiryRoomMessagesWithoutMembership() throws Exception {
            User anotherAdmin = persist(UserFixture.createAdmin(university));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());
            int chatRoomId = parseChatRoomId(
                performPost("/chats/rooms/admin")
                    .andExpect(status().isOk())
                    .andReturn()
            );

            performPost("/chats/rooms/" + chatRoomId + "/messages",
                new ChatMessageSendRequest("문의 내용입니다"))
                .andExpect(status().isOk());

            clearPersistenceContext();

            mockLoginUser(anotherAdmin.getId());
            performGet("/chats/rooms/" + chatRoomId + "?page=1&limit=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.messages[0].content").value("문의 내용입니다"))
                .andExpect(jsonPath("$.messages[0].isMine").value(false));

            assertThat(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .extracting(ChatRoomMember::getUserId)
                .containsExactlyInAnyOrder(SYSTEM_ADMIN_ID, normalUser.getId());
        }

        @Test
        @DisplayName("어드민이 나간 문의 채팅방에 사용자가 새 메시지를 보내 어드민 목록에 다시 노출된다")
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        @Sql(
            statements = CHAT_TEST_DATA_CLEANUP_SQL,
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
        )
        void adminLeftInquiryRoomReappearsWhenUserSendsNewMessage() throws Exception {
            // given - 문의 채팅방 생성 (일반 사용자 -> system admin)
            mockLoginUser(normalUser.getId());
            var createResult = performPost("/chats/rooms/admin")
                .andExpect(status().isOk())
                .andReturn();
            int chatRoomId = parseChatRoomId(createResult);

            // 사용자가 메시지 전송 (목록에 노출되기 위한 조건)
            performPost("/chats/rooms/" + chatRoomId + "/messages",
                new ChatMessageSendRequest("첫 문의 메시지입니다"))
                .andExpect(status().isOk());

            // system admin(ID=1)이 목록에서 방을 확인
            mockLoginUser(SYSTEM_ADMIN_ID);
            var adminRoomsBefore = performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andReturn();
            assertThat(extractRoomIds(adminRoomsBefore)).contains(chatRoomId);

            // system admin(ID=1)이 문의 채팅방 나가기
            performDelete("/chats/rooms/" + chatRoomId)
                .andExpect(status().isNoContent());

            // when - system admin이 목록 조회하면 나간 방은 안 보임
            var adminRoomsAfterLeave = performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andReturn();
            assertThat(extractRoomIds(adminRoomsAfterLeave)).doesNotContain(chatRoomId);

            // 사용자가 다시 메시지 전송
            mockLoginUser(normalUser.getId());
            performPost("/chats/rooms/" + chatRoomId + "/messages",
                new ChatMessageSendRequest("추가 문의 메시지입니다"))
                .andExpect(status().isOk());

            // lastMessageSentAt 강제 업데이트 (테스트 트랜잭션 롤백으로 인한 workaround)
            entityManager.createNativeQuery(
                "UPDATE chat_room SET last_message_sent_at = CURRENT_TIMESTAMP WHERE id = ?"
            ).setParameter(1, chatRoomId).executeUpdate();
            entityManager.flush();

            // then - system admin이 목록 조회하면 다시 보임
            mockLoginUser(SYSTEM_ADMIN_ID);
            var adminRoomsAfterNewMessage = performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andReturn();
            assertThat(extractRoomIds(adminRoomsAfterNewMessage)).contains(chatRoomId);
        }

        private int parseChatRoomId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
            String responseBody = result.getResponse().getContentAsString();
            return objectMapper.readTree(responseBody).get("chatRoomId").asInt();
        }

        private List<Integer> extractRoomIds(org.springframework.test.web.servlet.MvcResult result) throws Exception {
            String responseBody = result.getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode rooms = root.get("rooms");
            List<Integer> roomIds = new java.util.ArrayList<>();
            if (rooms != null && rooms.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode room : rooms) {
                    // roomId 또는 chatRoomId 필드 확인
                    com.fasterxml.jackson.databind.JsonNode roomIdNode =
                        room.has("chatRoomId") ? room.get("chatRoomId") : room.get("roomId");
                    if (roomIdNode != null) {
                        roomIds.add(roomIdNode.asInt());
                    }
                }
            }
            return roomIds;
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

            ChatRoom bcsdRoom = persist(ChatRoom.clubGroupOf(bcsd));
            ChatRoom cseRoom = persist(ChatRoom.clubGroupOf(cse));
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
        @DisplayName("메시지를 전송하면 chat_room last message 메타데이터도 함께 갱신된다")
        @Sql(
            statements = CHAT_TEST_DATA_CLEANUP_SQL,
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
        )
        void sendMessageUpdatesChatRoomLastMessageColumns() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);
            mockLoginUser(normalUser.getId());

            // when
            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest("메타데이터 확인"))
                .andExpect(status().isOk());

            // then
            TestTransaction.flagForCommit();
            TestTransaction.end();

            transactionTemplate.execute(status -> {
                clearPersistenceContext();
                ChatRoom updatedRoom = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
                assertThat(updatedRoom.getLastMessageContent()).isEqualTo("메타데이터 확인");
                assertThat(updatedRoom.getLastMessageSentAt()).isNotNull();
                return null;
            });
        }

        @Test
        @DisplayName("관리자가 문의방에 답변하면 실제 문의 사용자에게 알림을 보낸다")
        void adminReplySendsNotificationToInquiryUser() throws Exception {
            User anotherAdmin = persist(UserFixture.createAdmin(university));
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());
            int roomId = objectMapper.readTree(
                performPost("/chats/rooms/admin")
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
            ).get("chatRoomId").asInt();

            clearInvocations(notificationService);

            mockLoginUser(anotherAdmin.getId());
            performPost("/chats/rooms/" + roomId + "/messages", new ChatMessageSendRequest("관리자 답변입니다"))
                .andExpect(status().isOk());

            verify(notificationService)
                .sendChatNotification(normalUser.getId(), roomId, anotherAdmin.getName(), "관리자 답변입니다");
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
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    class LeaveChatRoom {

        @BeforeEach
        void setUpLeaveFixture() {
            transactionTemplate.execute(status -> {
                targetUser = createUser("상대유저", "2021136002");
                clearPersistenceContext();
                return null;
            });
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("1:1 채팅방을 나가면 목록에서 숨겨지고 새 메시지부터 다시 보인다")
        void leaveDirectChatRoomAndShowOnlyNewMessages() throws Exception {
            // 트랜잭션 내에서 테스트 데이터 생성 (NOT_SUPPORTED 테스트는 트랜잭션 없이 실행됨)
            ChatRoom[] chatRoomHolder = new ChatRoom[1];
            transactionTemplate.execute(status -> {
                chatRoomHolder[0] = createDirectChatRoom(normalUser, targetUser);
                return null;
            });
            ChatRoom chatRoom = chatRoomHolder[0];

            mockLoginUser(normalUser.getId());
            performPost("/chats/rooms/" + chatRoom.getId() + "/messages", new ChatMessageSendRequest("첫 메시지"))
                .andExpect(status().isOk());

            performDelete("/chats/rooms/" + chatRoom.getId())
                .andExpect(status().isNoContent());

            transactionTemplate.execute(status -> {
                clearPersistenceContext();
                ChatRoomMember leftMember = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoom.getId(), normalUser.getId())
                    .orElseThrow();
                assertThat(leftMember.hasLeft()).isTrue();
                return null;
            });

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

            // 트랜잭션 커밋
            TestTransaction.flagForCommit();
            TestTransaction.end();

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
            ChatRoom groupRoom = persist(ChatRoom.clubGroupOf(club));
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
        @Sql(
            statements = CHAT_TEST_DATA_CLEANUP_SQL,
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
        )
        void getMessagesForbidden() throws Exception {
            // given
            ChatRoom chatRoom = createDirectChatRoom(normalUser, targetUser);

            TestTransaction.flagForCommit();
            TestTransaction.end();

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
            ChatRoom groupRoom = persist(ChatRoom.clubGroupOf(developmentClub));
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
                .andExpect(jsonPath("$.roomMatches.rooms[1].chatType").value("CLUB_GROUP"))
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
            ChatRoom groupRoom = persist(ChatRoom.clubGroupOf(developmentClub));
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
            ChatRoom groupRoom = persist(ChatRoom.clubGroupOf(developmentClub));
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

    @Nested
    @DisplayName("POST /chats/rooms/group - 그룹 채팅방 생성")
    class CreateGroupChatRoom {

        private User memberA;
        private User memberB;

        @BeforeEach
        void setUpGroupChatFixture() {
            memberA = createUser("멤버A", "2021136002");
            memberB = createUser("멤버B", "2021136003");
            clearPersistenceContext();
        }

        @Test
        @DisplayName("그룹 채팅방을 생성하면 방장과 멤버가 모두 참여한다")
        void createGroupChatRoomSuccess() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when
            int roomId = objectMapper.readTree(
                performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                    List.of(memberA.getId(), memberB.getId())
                ))
                    .andReturn().getResponse().getContentAsString()
            ).get("chatRoomId").asInt();

            // then - 채팅방 타입이 GROUP인지 확인
            clearPersistenceContext();
            ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow();
            assertThat(chatRoom.getRoomType()).isEqualTo(ChatType.GROUP);

            // then - 방장(owner) 확인
            ChatRoomMember ownerMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(roomId, normalUser.getId()).orElseThrow();
            assertThat(ownerMember.isOwner()).isTrue();

            // then - 일반 멤버 확인
            ChatRoomMember memberARecord = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(roomId, memberA.getId()).orElseThrow();
            assertThat(memberARecord.isOwner()).isFalse();

            ChatRoomMember memberBRecord = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(roomId, memberB.getId()).orElseThrow();
            assertThat(memberBRecord.isOwner()).isFalse();

            assertThat(chatRoomMemberRepository.findByChatRoomId(roomId)).hasSize(3);
        }

        @Test
        @DisplayName("userIds에 자신만 포함되면 400을 반환한다")
        void createGroupChatRoomWithSelfOnlyFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                List.of(normalUser.getId())
            ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_CREATE_CHAT_ROOM_WITH_SELF"));
        }

        @Test
        @DisplayName("userIds가 빈 리스트이면 validation 에러를 반환한다")
        void createGroupChatRoomWithEmptyUserIdsFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
        }

        @Test
        @DisplayName("존재하지 않는 userId가 포함되면 404를 반환한다")
        void createGroupChatRoomWithNonExistentUserFails() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then
            performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                List.of(memberA.getId(), 99999)
            ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_USER"));
        }

        @Test
        @DisplayName("중복 userId는 무시하고 정상 생성한다")
        void createGroupChatRoomWithDuplicateUserIds() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when
            int roomId = objectMapper.readTree(
                performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                    List.of(memberA.getId(), memberA.getId(), memberB.getId())
                ))
                    .andReturn().getResponse().getContentAsString()
            ).get("chatRoomId").asInt();

            // then - 멤버 수가 중복 제거된 3명(방장 + A + B)이어야 한다
            clearPersistenceContext();
            assertThat(chatRoomMemberRepository.findByChatRoomId(roomId)).hasSize(3);
        }

        @Test
        @DisplayName("userIds에 자신이 포함되어도 무시하고 정상 생성한다")
        void createGroupChatRoomWithSelfInUserIds() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when
            int roomId = objectMapper.readTree(
                performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                    List.of(normalUser.getId(), memberA.getId())
                ))
                    .andReturn().getResponse().getContentAsString()
            ).get("chatRoomId").asInt();

            // then - 멤버 수가 2명(방장 + A)이어야 한다
            clearPersistenceContext();
            assertThat(chatRoomMemberRepository.findByChatRoomId(roomId)).hasSize(2);
        }

        @Test
        @DisplayName("초대받은 멤버도 그룹 채팅방에 메시지를 전송할 수 있다")
        void invitedMemberCanSendMessageToGroupChatRoom() throws Exception {
            // given
            mockLoginUser(normalUser.getId());
            int roomId = objectMapper.readTree(
                performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                    List.of(memberA.getId(), memberB.getId())
                ))
                    .andReturn().getResponse().getContentAsString()
            ).get("chatRoomId").asInt();

            // when & then - memberA(초대받은 멤버)가 메시지 전송
            mockLoginUser(memberA.getId());
            performPost(
                "/chats/rooms/" + roomId + "/messages",
                new ChatMessageSendRequest("초대받은 멤버의 메시지")
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderId").value(memberA.getId()))
                .andExpect(jsonPath("$.senderName").value(memberA.getName()))
                .andExpect(jsonPath("$.content").value("초대받은 멤버의 메시지"))
                .andExpect(jsonPath("$.isMine").value(true));
        }

        @Test
        @DisplayName("같은 유저들로 그룹 채팅방을 여러 개 생성할 수 있다")
        void createMultipleGroupChatRoomsWithSameUsers() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when - 같은 유저들로 첫 번째 그룹방 생성
            int firstRoomId = objectMapper.readTree(
                performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                    List.of(memberA.getId(), memberB.getId())
                ))
                    .andReturn().getResponse().getContentAsString()
            ).get("chatRoomId").asInt();

            // when - 같은 유저들로 두 번째 그룹방 생성
            int secondRoomId = objectMapper.readTree(
                performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                    List.of(memberA.getId(), memberB.getId())
                ))
                    .andReturn().getResponse().getContentAsString()
            ).get("chatRoomId").asInt();

            // then - 서로 다른 방이 생성됨 (DIRECT와 달리 중복 허용)
            assertThat(secondRoomId).isNotEqualTo(firstRoomId);
            clearPersistenceContext();
            assertThat(chatRoomRepository.findGroupRoomsByMemberUserId(normalUser.getId())).hasSize(2);
        }

        @Test
        @DisplayName("생성된 그룹 채팅방에 메시지를 전송할 수 있다")
        void canSendMessageToCreatedGroupChatRoom() throws Exception {
            // given
            mockLoginUser(normalUser.getId());
            int roomId = objectMapper.readTree(
                performPost("/chats/rooms/group", new ChatRoomCreateRequest.Group(
                    List.of(memberA.getId(), memberB.getId())
                ))
                    .andReturn().getResponse().getContentAsString()
            ).get("chatRoomId").asInt();

            // when & then
            performPost(
                "/chats/rooms/" + roomId + "/messages",
                new ChatMessageSendRequest("그룹방 첫 메시지")
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").isNumber())
                .andExpect(jsonPath("$.senderId").value(normalUser.getId()))
                .andExpect(jsonPath("$.senderName").value(normalUser.getName()))
                .andExpect(jsonPath("$.content").value("그룹방 첫 메시지"))
                .andExpect(jsonPath("$.isMine").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /chats/rooms/{chatRoomId}/members/{targetUserId} - 멤버 강퇴")
    class KickMember {

        private User ownerUser;
        private User memberUser;
        private User anotherMember;
        private ChatRoom groupRoom;

        @BeforeEach
        void setUpKickFixture() {
            ownerUser = createUser("방장", "2021136002");
            memberUser = createUser("멤버", "2021136003");
            anotherMember = createUser("다른멤버", "2021136004");
            groupRoom = createGroupChatRoomWithOwner(ownerUser, memberUser, anotherMember);
            clearPersistenceContext();
        }

        @Test
        @DisplayName("방장이 일반 멤버를 강퇴한다")
        void kickMemberSuccess() throws Exception {
            // given
            mockLoginUser(ownerUser.getId());

            // when & then
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + memberUser.getId())
                .andExpect(status().isNoContent());

            // then
            clearPersistenceContext();
            assertThat(chatRoomMemberRepository.findByChatRoomIdAndUserId(
                groupRoom.getId(), memberUser.getId()
            )).isEmpty();
            assertThat(chatRoomMemberRepository.findByChatRoomIdAndUserId(
                groupRoom.getId(), ownerUser.getId()
            )).isPresent();
            assertThat(chatRoomMemberRepository.findByChatRoomId(groupRoom.getId())).hasSize(2);
        }

        @Test
        @DisplayName("방장이 아닌 멤버가 강퇴하면 403을 반환한다")
        void kickMemberByNonOwnerFails() throws Exception {
            // given
            mockLoginUser(memberUser.getId());

            // when & then
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + anotherMember.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_KICK"));
        }

        @Test
        @DisplayName("자기 자신을 강퇴하면 400을 반환한다")
        void kickSelfFails() throws Exception {
            // given
            mockLoginUser(ownerUser.getId());

            // when & then
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + ownerUser.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_KICK_SELF"));
        }

        @Test
        @DisplayName("1:1 채팅방에서 강퇴하면 400을 반환한다")
        void kickInDirectRoomFails() throws Exception {
            // given
            ChatRoom directRoom = createDirectChatRoom(ownerUser, memberUser);
            mockLoginUser(ownerUser.getId());

            // when & then
            performDelete("/chats/rooms/" + directRoom.getId() + "/members/" + memberUser.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_KICK_IN_NON_GROUP_ROOM"));
        }

        @Test
        @DisplayName("동아리 채팅방에서 강퇴하면 400을 반환한다")
        void kickInClubRoomFails() throws Exception {
            // given
            Club club = persist(ClubFixture.create(university));
            ChatRoom clubRoom = persist(ChatRoom.clubGroupOf(club));
            addRoomMember(clubRoom, ownerUser);
            addRoomMember(clubRoom, memberUser);
            clearPersistenceContext();
            mockLoginUser(ownerUser.getId());

            // when & then
            performDelete("/chats/rooms/" + clubRoom.getId() + "/members/" + memberUser.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_KICK_IN_NON_GROUP_ROOM"));
        }

        @Test
        @DisplayName("다른 방장(owner) 멤버를 강퇴하면 400을 반환한다")
        void kickAnotherOwnerFails() throws Exception {
            // given - 방장 2명인 그룹 방을 수동 생성
            ChatRoom room = persist(ChatRoom.groupOf());
            ChatRoom managedRoom = entityManager.getReference(ChatRoom.class, room.getId());
            User managedOwner = entityManager.getReference(User.class, ownerUser.getId());
            User managedMember = entityManager.getReference(User.class, memberUser.getId());
            persist(ChatRoomMember.ofOwner(managedRoom, managedOwner, room.getCreatedAt()));
            persist(ChatRoomMember.ofOwner(managedRoom, managedMember, room.getCreatedAt()));
            clearPersistenceContext();
            mockLoginUser(ownerUser.getId());

            // when & then
            performDelete("/chats/rooms/" + room.getId() + "/members/" + memberUser.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_KICK_ROOM_OWNER"));
        }

        @Test
        @DisplayName("채팅방에 없는 멤버를 강퇴하면 403을 반환한다")
        void kickNonMemberTargetFails() throws Exception {
            // given
            User outsiderUser = createUser("외부인", "2021136005");
            clearPersistenceContext();
            mockLoginUser(ownerUser.getId());

            // when & then
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + outsiderUser.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("강퇴된 멤버는 채팅방 이름을 수정할 수 없다")
        void kickedMemberCannotUpdateRoomName() throws Exception {
            // given
            mockLoginUser(ownerUser.getId());
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + memberUser.getId())
                .andExpect(status().isNoContent());

            // when & then
            mockLoginUser(memberUser.getId());
            performPatch(
                "/chats/rooms/" + groupRoom.getId() + "/name",
                new ChatRoomNameUpdateRequest("강퇴 후 이름")
            )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("강퇴된 멤버는 메시지를 보낼 수 없다")
        void kickedMemberCannotSendMessage() throws Exception {
            // given
            Integer roomId = groupRoom.getId();
            mockLoginUser(ownerUser.getId());
            performDelete("/chats/rooms/" + roomId + "/members/" + memberUser.getId())
                .andExpect(status().isNoContent());

            // when & then
            mockLoginUser(memberUser.getId());
            performPost(
                "/chats/rooms/" + roomId + "/messages",
                new ChatMessageSendRequest("강퇴 후 메시지")
            )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("강퇴된 멤버는 메시지를 조회할 수 없다")
        @Sql(
            statements = CHAT_TEST_DATA_CLEANUP_SQL,
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
        )
        void kickedMemberCannotGetMessages() throws Exception {
            // given
            Integer roomId = groupRoom.getId();
            mockLoginUser(ownerUser.getId());
            performDelete("/chats/rooms/" + roomId + "/members/" + memberUser.getId())
                .andExpect(status().isNoContent());

            TestTransaction.flagForCommit();
            TestTransaction.end();

            // when & then
            mockLoginUser(memberUser.getId());
            performGet("/chats/rooms/" + roomId + "?page=1&limit=20")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("강퇴된 멤버의 방 목록에서 해당 방이 제거된다")
        void kickedMemberRoomRemovedFromList() throws Exception {
            // given
            mockLoginUser(ownerUser.getId());
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + memberUser.getId())
                .andExpect(status().isNoContent());

            // when & then
            mockLoginUser(memberUser.getId());
            performGet("/chats/rooms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[?(@.roomId==" + groupRoom.getId() + ")]").doesNotExist());
        }

        @Test
        @DisplayName("존재하지 않는 채팅방에서 강퇴하면 404를 반환한다")
        void kickInNonExistentRoomFails() throws Exception {
            // given
            mockLoginUser(ownerUser.getId());

            // when & then
            performDelete("/chats/rooms/99999/members/" + memberUser.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_CHAT_ROOM"));
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 사용자가 강퇴를 시도하면 403을 반환한다")
        void kickByOutsiderRequesterFails() throws Exception {
            // given
            User outsiderUser = createUser("외부인", "2021136005");
            clearPersistenceContext();
            mockLoginUser(outsiderUser.getId());

            // when & then
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + memberUser.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("이미 나간 멤버를 강퇴하면 403을 반환한다")
        void kickAlreadyLeftMemberFails() throws Exception {
            // given - member leaves the group room
            mockLoginUser(memberUser.getId());
            performDelete("/chats/rooms/" + groupRoom.getId())
                .andExpect(status().isNoContent());

            // when & then - owner tries to kick the left member
            mockLoginUser(ownerUser.getId());
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + memberUser.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("마지막 멤버를 강퇴하면 방에 방장만 남는다")
        void kickLastMemberLeavesOwnerOnly() throws Exception {
            // given - room with only owner and one member
            User soleMember = createUser("유일멤버", "2021136005");
            ChatRoom twoPersonRoom = createGroupChatRoomWithOwner(ownerUser, soleMember);
            clearPersistenceContext();

            mockLoginUser(ownerUser.getId());

            // when
            performDelete("/chats/rooms/" + twoPersonRoom.getId() + "/members/" + soleMember.getId())
                .andExpect(status().isNoContent());

            // then - only owner remains
            clearPersistenceContext();
            assertThat(chatRoomMemberRepository.findByChatRoomId(twoPersonRoom.getId())).hasSize(1);
            assertThat(chatRoomMemberRepository.findByChatRoomIdAndUserId(twoPersonRoom.getId(), ownerUser.getId()))
                .isPresent()
                .get()
                .extracting(ChatRoomMember::isOwner)
                .isEqualTo(true);
        }

        @Test
        @DisplayName("방장이 나간 그룹방에서 일반 멤버가 강퇴할 수 없다")
        void kickFailsAfterOwnerLeaves() throws Exception {
            // given - 3인 그룹방: owner + memberUser + anotherMember
            // owner가 나가면 방장이 없는 상태가 됨
            mockLoginUser(ownerUser.getId());
            performDelete("/chats/rooms/" + groupRoom.getId())
                .andExpect(status().isNoContent());

            // when & then - 남은 일반 멤버가 다른 멤버를 강퇴 시도
            mockLoginUser(memberUser.getId());
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + anotherMember.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_KICK"));
        }
    }

    @Nested
    @DisplayName("GET /chats/rooms/{chatRoomId} - 메시지 조회 페이지네이션 엣지 케이스")
    @Sql(
        statements = CHAT_TEST_DATA_CLEANUP_SQL,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    class GetMessagesPaginationEdgeCases {

        private ChatRoom directRoom;
        private User chatPartner;

        @BeforeEach
        void setUpPaginationFixture() {
            chatPartner = createUser("채팅상대", "2021136006");
            clearPersistenceContext();
        }

        @Test
        @DisplayName("빈 채팅방의 메시지를 조회하면 빈 목록을 반환한다")
        void getMessagesFromEmptyRoomReturnsEmptyList() throws Exception {
            // given - 메시지가 없는 새로 생성된 방
            directRoom = createDirectChatRoom(normalUser, chatPartner);
            TestTransaction.flagForCommit();
            TestTransaction.end();

            mockLoginUser(normalUser.getId());

            // when & then
            performGet("/chats/rooms/" + directRoom.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages").isEmpty())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPage").value(0));
        }

        @Test
        @DisplayName("존재하지 않는 페이지를 조회하면 빈 목록을 반환한다")
        void getMessagesFromNonExistentPageReturnsEmptyList() throws Exception {
            // given - 메시지 5개 생성
            directRoom = createDirectChatRoom(normalUser, chatPartner);
            for (int i = 1; i <= 5; i++) {
                persistChatMessage(directRoom, chatPartner, "메시지 " + i);
            }
            TestTransaction.flagForCommit();
            TestTransaction.end();

            mockLoginUser(normalUser.getId());

            // when & then - 100페이지 조회 (존재하지 않음)
            performGet("/chats/rooms/" + directRoom.getId() + "?page=100&limit=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages").isEmpty())
                .andExpect(jsonPath("$.currentPage").value(100))
                .andExpect(jsonPath("$.totalPage").value(1));
        }

        @Test
        @DisplayName("마지막 페이지는 부분 결과를 반환할 수 있다")
        void getMessagesLastPageReturnsPartialResults() throws Exception {
            // given - 메시지 25개 생성
            directRoom = createDirectChatRoom(normalUser, chatPartner);
            for (int i = 1; i <= 25; i++) {
                persistChatMessage(directRoom, chatPartner, "메시지 " + i);
            }
            TestTransaction.flagForCommit();
            TestTransaction.end();

            mockLoginUser(normalUser.getId());

            // when & then - limit=10, page=3 (21-25번 메시지, 5개만 반환)
            performGet("/chats/rooms/" + directRoom.getId() + "?page=3&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages").value(org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$.currentPage").value(3))
                .andExpect(jsonPath("$.totalPage").value(3));
        }
    }

    @Nested
    @DisplayName("POST /chats/rooms/{chatRoomId}/messages - 메시지 전송 권한 및 엣지 케이스")
    @Sql(
        statements = CHAT_TEST_DATA_CLEANUP_SQL,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    class SendMessageEdgeCases {

        private ChatRoom groupRoom;
        private User roomOwner;
        private User roomMember;
        private User nonMember;

        @BeforeEach
        void setUpSendMessageEdgeFixture() {
            roomOwner = createUser("방장", "2021136007");
            roomMember = createUser("방멤버", "2021136008");
            nonMember = createUser("비멤버", "2021136009");
            clearPersistenceContext();
        }

        @Test
        @DisplayName("정확히 1000자 메시지는 전송 성공한다")
        void sendMessageExactly1000CharsSuccess() throws Exception {
            // given
            groupRoom = createGroupChatRoomWithOwner(roomOwner, roomMember);
            String exactly1000Chars = "a".repeat(1000);
            TestTransaction.flagForCommit();
            TestTransaction.end();

            mockLoginUser(roomMember.getId());

            // when & then
            performPost("/chats/rooms/" + groupRoom.getId() + "/messages",
                new ChatMessageSendRequest(exactly1000Chars))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(exactly1000Chars));
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 사용자는 메시지를 전송할 수 없다")
        void sendMessageByNonMemberReturnsForbidden() throws Exception {
            // given
            groupRoom = createGroupChatRoomWithOwner(roomOwner, roomMember);
            TestTransaction.flagForCommit();
            TestTransaction.end();

            mockLoginUser(nonMember.getId());

            // when & then
            performPost("/chats/rooms/" + groupRoom.getId() + "/messages",
                new ChatMessageSendRequest("Unauthorized message"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("강퇴된 멤버는 메시지를 전송할 수 없다")
        void sendMessageByKickedMemberReturnsForbidden() throws Exception {
            // given - 방장이 멤버를 강퇴
            groupRoom = createGroupChatRoomWithOwner(roomOwner, roomMember);
            TestTransaction.flagForCommit();
            TestTransaction.end();

            mockLoginUser(roomOwner.getId());
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + roomMember.getId())
                .andExpect(status().isNoContent());

            // when & then - 강퇴된 멤버가 메시지 전송 시도
            mockLoginUser(roomMember.getId());
            performPost("/chats/rooms/" + groupRoom.getId() + "/messages",
                new ChatMessageSendRequest("Kicked member message"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }
    }

    @Nested
    @DisplayName("POST /chats/rooms/{chatRoomId}/mute - 채팅방 뮤트 권한 케이스")
    @Sql(
        statements = CHAT_TEST_DATA_CLEANUP_SQL,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    class ToggleMutePermissionCases {

        private ChatRoom groupRoom;
        private User roomOwner;
        private User roomMember;
        private User nonMember;

        @BeforeEach
        void setUpMutePermissionFixture() {
            roomOwner = createUser("방장", "2021136010");
            roomMember = createUser("방멤버", "2021136011");
            nonMember = createUser("비멤버", "2021136012");
            clearPersistenceContext();
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 사용자는 뮤트 설정을 변경할 수 없다")
        void toggleMuteByNonMemberReturnsForbidden() throws Exception {
            // given
            groupRoom = createGroupChatRoomWithOwner(roomOwner, roomMember);
            TestTransaction.flagForCommit();
            TestTransaction.end();

            mockLoginUser(nonMember.getId());

            // when & then
            performPost("/chats/rooms/" + groupRoom.getId() + "/mute")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }

        @Test
        @DisplayName("강퇴된 멤버는 뮤트 설정을 변경할 수 없다")
        void toggleMuteByKickedMemberReturnsForbidden() throws Exception {
            // given - 방장이 멤버를 강퇴
            groupRoom = createGroupChatRoomWithOwner(roomOwner, roomMember);
            TestTransaction.flagForCommit();
            TestTransaction.end();

            mockLoginUser(roomOwner.getId());
            performDelete("/chats/rooms/" + groupRoom.getId() + "/members/" + roomMember.getId())
                .andExpect(status().isNoContent());

            // when & then - 강퇴된 멤버가 뮤트 토글 시도
            mockLoginUser(roomMember.getId());
            performPost("/chats/rooms/" + groupRoom.getId() + "/mute")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_CHAT_ROOM_ACCESS"));
        }
    }

    @Nested
    @DisplayName("GET /chats/rooms/search - 채팅 검색 엣지 케이스")
    class SearchChatsEdgeCases {

        private ChatRoom directRoom;
        private User chatPartner;

        @BeforeEach
        void setUpSearchEdgeFixture() {
            chatPartner = createUser("검색상대", "2021136013");
            directRoom = createDirectChatRoom(normalUser, chatPartner);
            persistChatMessage(directRoom, chatPartner, "검색 가능한 메시지");
            clearPersistenceContext();
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
        void searchChatsWithNoMatchesReturnsEmpty() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then - 존재하지 않는 키워드로 검색
            performGet("/chats/rooms/search?keyword=존재하지않는키워드12345&page=1&limit=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomMatches.rooms").isArray())
                .andExpect(jsonPath("$.roomMatches.rooms").isEmpty())
                .andExpect(jsonPath("$.messageMatches.messages").isArray())
                .andExpect(jsonPath("$.messageMatches.messages").isEmpty());
        }

        @Test
        @DisplayName("한 글자 키워드로 검색해도 200을 반환한다")
        void searchChatsWithSingleCharacterKeywordReturnsOk() throws Exception {
            // given
            mockLoginUser(normalUser.getId());

            // when & then - 1글자 키워드로 검색
            performGet("/chats/rooms/search?keyword=a&page=1&limit=20")
                .andExpect(status().isOk());
        }
    }
}
