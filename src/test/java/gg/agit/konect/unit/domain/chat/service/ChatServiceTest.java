package gg.agit.konect.unit.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_IN_NON_GROUP_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_ROOM_OWNER;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_LEAVE_GROUP_CHAT_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_KICK;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ChatMuteResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatInviteQueryRepository;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.chat.service.ChatService;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatServiceTest extends ServiceTestSupport {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private NotificationMuteSettingRepository notificationMuteSettingRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ChatInviteQueryRepository chatInviteQueryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatPresenceService chatPresenceService;

    @Mock
    private ChatRoomMembershipService chatRoomMembershipService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("createOrGetChatRoom은 자기 자신과의 direct room 생성을 거부한다")
    void createOrGetChatRoomRejectsSelfChat() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "요청자", UserRole.USER);
        given(userRepository.getById(userId)).willReturn(user);
        given(userRepository.getById(userId)).willReturn(user);

        // when & then
        assertErrorCode(
            () -> chatService.createOrGetChatRoom(userId, new ChatRoomCreateRequest(userId)),
            CANNOT_CREATE_CHAT_ROOM_WITH_SELF
        );
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("createOrGetChatRoom은 기존 direct room이 있으면 재사용하고 요청자 멤버십을 복구한다")
    void createOrGetChatRoomReusesExistingDirectRoomAndReopensRequesterMembership() {
        // given
        Integer currentUserId = 10;
        Integer targetUserId = 20;
        User currentUser = createUser(currentUserId, "요청자", UserRole.USER);
        User targetUser = createUser(targetUserId, "상대", UserRole.USER);
        ChatRoom room = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember requesterMember = createRoomMember(room, currentUser, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ReflectionTestUtils.setField(requesterMember, "leftAt", LocalDateTime.of(2026, 4, 11, 11, 0));
        ReflectionTestUtils.setField(requesterMember, "visibleMessageFrom", LocalDateTime.of(2026, 4, 11, 11, 0));
        ChatRoomMember targetMember = createRoomMember(room, targetUser, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(userRepository.getById(currentUserId)).willReturn(currentUser);
        given(userRepository.getById(targetUserId)).willReturn(targetUser);
        given(chatRoomRepository.findByTwoUsers(currentUserId, targetUserId, ChatType.DIRECT))
            .willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), currentUserId))
            .willReturn(Optional.of(requesterMember));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), targetUserId))
            .willReturn(Optional.of(targetMember));

        // when
        ChatRoomResponse response = chatService.createOrGetChatRoom(currentUserId, new ChatRoomCreateRequest(targetUserId));

        // then
        assertThat(response.chatRoomId()).isEqualTo(room.getId());
        assertThat(requesterMember.hasLeft()).isFalse();
        assertThat(requesterMember.getVisibleMessageFrom()).isNotNull();
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("createOrGetChatRoom은 admin이 일반 사용자와 채팅할 때 system-admin room 경로를 사용한다")
    void createOrGetChatRoomUsesSystemAdminRoomForAdminToUser() {
        // given
        Integer adminUserId = 99;
        Integer targetUserId = 20;
        User adminUser = createUser(adminUserId, "관리자", UserRole.ADMIN);
        User targetUser = createUser(targetUserId, "일반 사용자", UserRole.USER);
        User systemAdmin = createUser(SYSTEM_ADMIN_ID, "시스템관리자", UserRole.ADMIN);
        ChatRoom room = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        given(userRepository.getById(adminUserId)).willReturn(adminUser);
        given(userRepository.getById(targetUserId)).willReturn(targetUser);
        given(chatRoomRepository.findByTwoUsers(SYSTEM_ADMIN_ID, targetUserId, ChatType.DIRECT))
            .willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(room);
        given(userRepository.getById(SYSTEM_ADMIN_ID)).willReturn(systemAdmin);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), SYSTEM_ADMIN_ID))
            .willReturn(Optional.empty());
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), targetUserId))
            .willReturn(Optional.empty());
        given(chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(List.of(room.getId())))
            .willReturn(List.of(
                new Object[]{room.getId(), SYSTEM_ADMIN_ID, room.getCreatedAt()},
                new Object[]{room.getId(), targetUserId, room.getCreatedAt()}
            ));

        // when
        ChatRoomResponse response = chatService.createOrGetChatRoom(adminUserId, new ChatRoomCreateRequest(targetUserId));

        // then
        assertThat(response.chatRoomId()).isEqualTo(room.getId());
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
        verify(chatRoomMemberRepository, never()).findByChatRoomIdAndUserId(room.getId(), adminUserId);
    }

    @Test
    @DisplayName("createGroupChatRoom은 초대 대상을 dedup 하고 자기 자신을 제외한 뒤 owner/member를 저장한다")
    void createGroupChatRoomDeduplicatesInviteesAndSavesMembers() {
        // given
        Integer creatorId = 10;
        User creator = createUser(creatorId, "생성자", UserRole.USER);
        User user20 = createUser(20, "멤버1", UserRole.USER);
        User user30 = createUser(30, "멤버2", UserRole.USER);
        ChatRoom room = createRoom(50, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 12, 0));

        given(userRepository.getById(creatorId)).willReturn(creator);
        given(userRepository.findAllByIdIn(List.of(20, 30))).willReturn(List.of(user20, user30));
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(room);

        // when
        ChatRoomResponse response = chatService.createGroupChatRoom(
            creatorId,
            new ChatRoomCreateRequest.Group(List.of(creatorId, 20, 20, 30))
        );

        // then
        assertThat(response.chatRoomId()).isEqualTo(room.getId());
        ArgumentCaptor<List<ChatRoomMember>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatRoomMemberRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue())
            .extracting(ChatRoomMember::getUserId, ChatRoomMember::isOwner)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(creatorId, true),
                org.assertj.core.groups.Tuple.tuple(20, false),
                org.assertj.core.groups.Tuple.tuple(30, false)
            );
    }

    @Test
    @DisplayName("createGroupChatRoom은 자기 자신만 남거나 조회되지 않는 사용자가 있으면 실패한다")
    void createGroupChatRoomRejectsInvalidInvitees() {
        // given
        Integer creatorId = 10;
        User creator = createUser(creatorId, "생성자", UserRole.USER);
        given(userRepository.getById(creatorId)).willReturn(creator);
        given(userRepository.findAllByIdIn(List.of(20, 30))).willReturn(List.of(createUser(20, "멤버1", UserRole.USER)));

        // when & then
        assertErrorCode(
            () -> chatService.createGroupChatRoom(creatorId, new ChatRoomCreateRequest.Group(List.of(creatorId))),
            CANNOT_CREATE_CHAT_ROOM_WITH_SELF
        );
        assertErrorCode(
            () -> chatService.createGroupChatRoom(creatorId, new ChatRoomCreateRequest.Group(List.of(20, 30))),
            NOT_FOUND_USER
        );
    }

    @Test
    @DisplayName("leaveChatRoom은 club group room 나가기를 거부하고 direct room은 leftAt을 갱신한다")
    void leaveChatRoomRejectsClubRoomAndMarksDirectRoomLeft() {
        // given
        Integer userId = 10;
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoom clubRoom = createClubRoom(2, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember directMember = createRoomMember(directRoom, createUser(userId, "사용자", UserRole.USER), false,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(chatRoomRepository.findById(clubRoom.getId())).willReturn(Optional.of(clubRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(directRoom.getId(), userId))
            .willReturn(Optional.of(directMember));

        // when
        chatService.leaveChatRoom(userId, directRoom.getId());

        // then
        assertThat(directMember.hasLeft()).isTrue();
        assertThat(directMember.getVisibleMessageFrom()).isNotNull();
        assertErrorCode(() -> chatService.leaveChatRoom(userId, clubRoom.getId()), CANNOT_LEAVE_GROUP_CHAT_ROOM);
    }

    @Test
    @DisplayName("leaveChatRoom은 일반 group room에서는 membership 삭제를 수행한다")
    void leaveChatRoomDeletesMembershipForGroupRoom() {
        // given
        Integer userId = 10;
        ChatRoom groupRoom = createRoom(3, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(groupRoom, createUser(userId, "사용자", UserRole.USER), false,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId))
            .willReturn(Optional.of(member));

        // when
        chatService.leaveChatRoom(userId, groupRoom.getId());

        // then
        verify(chatRoomMemberRepository).deleteByChatRoomIdAndUserId(groupRoom.getId(), userId);
    }

    @Test
    @DisplayName("kickMember는 비그룹방, self kick, 방장 아님, owner target을 각각 거부한다")
    void kickMemberRejectsInvalidRequests() {
        // given
        Integer requesterId = 10;
        Integer targetId = 20;
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoom groupRoom = createRoom(2, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember nonOwnerRequester = createRoomMember(groupRoom, createUser(requesterId, "요청자", UserRole.USER), false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember ownerTarget = createRoomMember(groupRoom, createUser(targetId, "방장", UserRole.USER), true,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), requesterId))
            .willReturn(Optional.of(nonOwnerRequester));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), targetId))
            .willReturn(Optional.of(ownerTarget));

        // when & then
        assertErrorCode(() -> chatService.kickMember(requesterId, directRoom.getId(), targetId), CANNOT_KICK_IN_NON_GROUP_ROOM);
        assertErrorCode(() -> chatService.kickMember(requesterId, groupRoom.getId(), requesterId), CANNOT_KICK_SELF);
        assertErrorCode(() -> chatService.kickMember(requesterId, groupRoom.getId(), targetId), FORBIDDEN_CHAT_ROOM_KICK);

        ChatRoomMember ownerRequester = createRoomMember(groupRoom, createUser(requesterId, "방장", UserRole.USER), true,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), requesterId))
            .willReturn(Optional.of(ownerRequester));
        assertErrorCode(() -> chatService.kickMember(requesterId, groupRoom.getId(), targetId), CANNOT_KICK_ROOM_OWNER);
    }

    @Test
    @DisplayName("kickMember는 유효한 group room에서 target membership을 삭제한다")
    void kickMemberDeletesTargetMembershipWhenValid() {
        // given
        Integer requesterId = 10;
        Integer targetId = 20;
        ChatRoom groupRoom = createRoom(2, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember ownerRequester = createRoomMember(groupRoom, createUser(requesterId, "방장", UserRole.USER), true,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember target = createRoomMember(groupRoom, createUser(targetId, "멤버", UserRole.USER), false,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), requesterId))
            .willReturn(Optional.of(ownerRequester));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), targetId))
            .willReturn(Optional.of(target));

        // when
        chatService.kickMember(requesterId, groupRoom.getId(), targetId);

        // then
        verify(chatRoomMemberRepository).deleteByChatRoomIdAndUserId(groupRoom.getId(), targetId);
    }

    @Test
    @DisplayName("toggleMute는 기존 setting이 있으면 토글하고 없으면 muted=true 로 저장한다")
    void toggleMuteTogglesExistingSettingOrCreatesNewOne() {
        // given
        Integer userId = 10;
        Integer roomId = 1;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom room = createRoom(roomId, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(room, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        NotificationMuteSetting setting = NotificationMuteSetting.of(NotificationTargetType.CHAT_ROOM, roomId, user, false);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(NotificationTargetType.CHAT_ROOM, roomId, userId))
            .willReturn(Optional.of(setting), Optional.empty());

        // when
        ChatMuteResponse toggled = chatService.toggleMute(userId, roomId);
        ChatMuteResponse created = chatService.toggleMute(userId, roomId);

        // then
        assertThat(toggled.isMuted()).isTrue();
        assertThat(created.isMuted()).isTrue();
        verify(notificationMuteSettingRepository, times(2)).save(any(NotificationMuteSetting.class));
    }

    @Test
    @DisplayName("getMessages는 direct room에서 direct 전용 readAt 갱신 경로를 사용한다")
    void getMessagesUsesDirectReadPath() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember directMember = createRoomMember(directRoom, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage directMessage = createMessage(100, directRoom, createUser(20, "상대", UserRole.USER), "direct", LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomId(directRoom.getId())).willReturn(List.of(directMember));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(directRoom.getId(), userId)).willReturn(Optional.of(directMember));
        given(chatMessageRepository.findByChatRoomId(eq(directRoom.getId()), nullable(LocalDateTime.class), eq(PageRequest.of(0, 20))))
            .willReturn(new PageImpl<>(List.of(directMessage), PageRequest.of(0, 20), 1));

        // when
        ChatMessagePageResponse response = chatService.getMessages(userId, directRoom.getId(), 1, 20);

        // then
        assertThat(response.messages()).hasSize(1);
        verify(chatRoomMembershipService).updateDirectRoomLastReadAt(eq(directRoom.getId()), eq(user), any(LocalDateTime.class), eq(directRoom));
        verify(chatPresenceService).recordPresence(directRoom.getId(), userId);
    }

    @Test
    @DisplayName("getMessages는 club group room에서 club membership 보정과 일반 readAt 갱신을 수행한다")
    void getMessagesUsesClubGroupReadPath() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom clubRoom = createClubRoom(2, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember clubRoomMember = createRoomMember(clubRoom, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage clubMessage = createMessage(200, clubRoom, user, "club", LocalDateTime.of(2026, 4, 11, 10, 2));

        given(chatRoomRepository.findById(clubRoom.getId())).willReturn(Optional.of(clubRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomId(clubRoom.getId())).willReturn(List.of(clubRoomMember));
        given(chatMessageRepository.countByChatRoomId(clubRoom.getId(), null)).willReturn(1L);
        given(chatMessageRepository.findByChatRoomId(eq(clubRoom.getId()), nullable(LocalDateTime.class), eq(PageRequest.of(0, 20))))
            .willReturn(new PageImpl<>(List.of(clubMessage), PageRequest.of(0, 20), 1));

        // when
        ChatMessagePageResponse response = chatService.getMessages(userId, clubRoom.getId(), 1, 20);

        // then
        assertThat(response.clubId()).isEqualTo(clubRoom.getClub().getId());
        verify(chatRoomMembershipService).ensureClubRoomMember(clubRoom.getId(), userId);
        verify(chatRoomMembershipService).updateLastReadAt(eq(clubRoom.getId()), eq(userId), any(LocalDateTime.class));
        verify(chatPresenceService).recordPresence(clubRoom.getId(), userId);
    }

    @Test
    @DisplayName("getMessages는 group room에서 접근 검증 후 일반 readAt 갱신 경로를 사용한다")
    void getMessagesUsesGroupReadPath() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom groupRoom = createRoom(3, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember groupMember = createRoomMember(groupRoom, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage groupMessage = createMessage(300, groupRoom, user, "group", LocalDateTime.of(2026, 4, 11, 10, 3));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId)).willReturn(Optional.of(groupMember));
        given(chatRoomMemberRepository.findByChatRoomId(groupRoom.getId())).willReturn(List.of(groupMember));
        given(chatMessageRepository.countByChatRoomId(groupRoom.getId(), null)).willReturn(1L);
        given(chatMessageRepository.findByChatRoomId(eq(groupRoom.getId()), nullable(LocalDateTime.class), eq(PageRequest.of(0, 20))))
            .willReturn(new PageImpl<>(List.of(groupMessage), PageRequest.of(0, 20), 1));

        // when
        ChatMessagePageResponse response = chatService.getMessages(userId, groupRoom.getId(), 1, 20);

        // then
        assertThat(response.clubId()).isNull();
        verify(chatRoomMembershipService).updateLastReadAt(eq(groupRoom.getId()), eq(userId), any(LocalDateTime.class));
        verify(chatPresenceService).recordPresence(groupRoom.getId(), userId);
    }

    @Test
    @DisplayName("getMessages는 group room 비회원 요청을 거부한다")
    void getMessagesRejectsGroupRoomOutsider() {
        // given
        Integer userId = 10;
        ChatRoom groupRoom = createRoom(3, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(userId)).willReturn(createUser(userId, "사용자", UserRole.USER));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId)).willReturn(Optional.empty());

        // when & then
        assertErrorCode(
            () -> chatService.getMessages(userId, groupRoom.getId(), 1, 20),
            FORBIDDEN_CHAT_ROOM_ACCESS
        );
    }

    private User createUser(Integer id, String name, UserRole role) {
        return UserFixture.createUserWithId(UniversityFixture.createWithId(1), id, name, "2024" + String.format("%04d", id), role);
    }

    private ChatRoom createRoom(Integer id, ChatType type, LocalDateTime createdAt) {
        ChatRoom room = switch (type) {
            case DIRECT -> ChatRoom.directOf();
            case GROUP -> ChatRoom.groupOf();
            case CLUB_GROUP -> ChatRoom.clubGroupOf(ClubFixture.createWithId(UniversityFixture.createWithId(1), 77));
        };
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", createdAt);
        return room;
    }

    private ChatRoom createClubRoom(Integer id, LocalDateTime createdAt) {
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 77, "BCSD");
        ChatRoom room = ChatRoom.clubGroupOf(club);
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", createdAt);
        return room;
    }

    private ChatRoomMember createRoomMember(ChatRoom room, User user, boolean isOwner, LocalDateTime lastReadAt) {
        ChatRoomMember member = isOwner ? ChatRoomMember.ofOwner(room, user, lastReadAt) : ChatRoomMember.of(room, user, lastReadAt);
        ReflectionTestUtils.setField(member, "createdAt", lastReadAt);
        return member;
    }

    private ChatMessage createMessage(Integer id, ChatRoom room, User sender, String content, LocalDateTime createdAt) {
        ChatMessage message = ChatMessage.of(room, sender, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        return message;
    }

    private void setCreatedAt(Object target, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(target, "createdAt", createdAt);
        ReflectionTestUtils.setField(target, "updatedAt", createdAt);
    }

    private void assertErrorCode(ThrowingCallable callable, ApiResponseCode errorCode) {
        assertThatThrownBy(callable)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode()).isEqualTo(errorCode));
    }
}
