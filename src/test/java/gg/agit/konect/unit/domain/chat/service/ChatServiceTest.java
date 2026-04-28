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
import static org.assertj.core.api.Assertions.tuple;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMuteResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomNameUpdateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.event.AdminChatReceivedEvent;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomQueryRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatDirectRoomAccessService;
import gg.agit.konect.domain.chat.service.ChatInviteService;
import gg.agit.konect.domain.chat.service.ChatMessageReadService;
import gg.agit.konect.domain.chat.service.ChatMessagePageResolver;
import gg.agit.konect.domain.chat.service.ChatMessageSendService;
import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.chat.service.ChatRoomAccessService;
import gg.agit.konect.domain.chat.service.ChatRoomCreationService;
import gg.agit.konect.domain.chat.service.ChatRoomMemberCommandService;
import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.chat.service.ChatRoomSummaryService;
import gg.agit.konect.domain.chat.service.ChatRoomSystemAdminService;
import gg.agit.konect.domain.chat.service.ChatSearchService;
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
    private ChatRoomQueryRepository chatRoomQueryRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private NotificationMuteSettingRepository notificationMuteSettingRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatPresenceService chatPresenceService;

    @Mock
    private ChatRoomMembershipService chatRoomMembershipService;

    @Mock
    private ChatRoomSummaryService chatRoomSummaryService;

    @Mock
    private ChatSearchService chatSearchService;

    @Mock
    private ChatInviteService chatInviteService;

    @Mock
    private ChatRoomSystemAdminService chatRoomSystemAdminService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        ChatDirectRoomAccessService chatDirectRoomAccessService =
            new ChatDirectRoomAccessService(chatRoomMemberRepository);
        ChatMessagePageResolver chatMessagePageResolver = new ChatMessagePageResolver(
            chatMessageRepository,
            chatRoomMemberRepository,
            clubMemberRepository,
            chatRoomSystemAdminService
        );
        ChatRoomAccessService chatRoomAccessService = new ChatRoomAccessService(
            chatRoomMemberRepository,
            userRepository,
            chatRoomMembershipService,
            chatRoomSystemAdminService,
            chatDirectRoomAccessService
        );
        ChatRoomMemberCommandService chatRoomMemberCommandService = new ChatRoomMemberCommandService(
            chatRoomRepository,
            chatRoomMemberRepository
        );
        ChatRoomMembershipService chatRoomMembershipForCreation = new ChatRoomMembershipService(
            chatRoomRepository,
            chatRoomMemberRepository,
            clubMemberRepository,
            userRepository,
            chatRoomSystemAdminService
        );
        ChatRoomCreationService chatRoomCreationService = new ChatRoomCreationService(
            chatRoomRepository,
            chatRoomMemberRepository,
            userRepository,
            chatRoomMembershipForCreation
        );
        ChatMessageSendService chatMessageSendService = new ChatMessageSendService(
            chatRoomRepository,
            chatMessageRepository,
            chatRoomMemberRepository,
            clubMemberRepository,
            userRepository,
            chatRoomSystemAdminService,
            chatDirectRoomAccessService,
            notificationService,
            eventPublisher
        );
        ChatMessageReadService chatMessageReadService = new ChatMessageReadService(
            chatMessageRepository,
            chatRoomMemberRepository,
            chatRoomSystemAdminService,
            chatDirectRoomAccessService
        );
        chatService = new ChatService(
            chatRoomRepository,
            chatRoomQueryRepository,
            chatMessageRepository,
            chatRoomMemberRepository,
            notificationMuteSettingRepository,
            clubMemberRepository,
            userRepository,
            chatPresenceService,
            chatRoomMembershipService,
            chatRoomMemberCommandService,
            chatRoomSummaryService,
            chatSearchService,
            chatInviteService,
            chatMessageReadService,
            chatMessagePageResolver,
            chatRoomAccessService,
            chatRoomCreationService,
            chatRoomSystemAdminService,
            chatDirectRoomAccessService,
            chatMessageSendService
        );
    }

    @Test
    @DisplayName("createOrGetChatRoom은 자기 자신과의 direct room 생성을 거부한다")
    void createOrGetChatRoomRejectsSelfChat() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "요청자", UserRole.USER);
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
        markMemberLeft(requesterMember, LocalDateTime.of(2026, 4, 11, 11, 0));
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
        ChatRoomResponse response = chatService.createOrGetChatRoom(currentUserId,
            new ChatRoomCreateRequest(targetUserId));

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
        int targetUserId = 20;
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
        given(chatRoomSystemAdminService.isSystemAdminRoom(room.getId())).willReturn(true);

        // when
        ChatRoomResponse response = chatService.createOrGetChatRoom(adminUserId,
            new ChatRoomCreateRequest(targetUserId));

        // then
        assertThat(response.chatRoomId()).isEqualTo(room.getId());
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
        verify(chatRoomMemberRepository, never()).findByChatRoomIdAndUserId(room.getId(), adminUserId);
    }

    @SuppressWarnings("unchecked")
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
    @DisplayName("kickMember는 비그룹방에서 멤버 강퇴를 거부한다")
    void kickMemberRejectsNonGroupRoom() {
        // given
        Integer requesterId = 10;
        Integer targetId = 20;
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));

        // when & then
        assertErrorCode(() -> chatService.kickMember(requesterId, directRoom.getId(), targetId),
            CANNOT_KICK_IN_NON_GROUP_ROOM);
    }

    @Test
    @DisplayName("kickMember는 자기 자신을 강퇴할 수 없다")
    void kickMemberRejectsSelfKick() {
        // given
        Integer requesterId = 10;
        ChatRoom groupRoom = createRoom(2, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));

        // when & then
        assertErrorCode(() -> chatService.kickMember(requesterId, groupRoom.getId(), requesterId), CANNOT_KICK_SELF);
    }

    @Test
    @DisplayName("kickMember는 방장이 아닌 요청자의 강퇴를 거부한다")
    void kickMemberRejectsNonOwnerRequester() {
        // given
        Integer requesterId = 10;
        Integer targetId = 20;
        ChatRoom groupRoom = createRoom(2, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember nonOwnerRequester = createRoomMember(groupRoom, createUser(requesterId, "요청자", UserRole.USER),
            false,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), requesterId))
            .willReturn(Optional.of(nonOwnerRequester));

        // when & then
        assertErrorCode(() -> chatService.kickMember(requesterId, groupRoom.getId(), targetId),
            FORBIDDEN_CHAT_ROOM_KICK);
    }

    @Test
    @DisplayName("kickMember는 방장을 강퇴할 수 없다")
    void kickMemberRejectsOwnerTarget() {
        // given
        Integer requesterId = 10;
        Integer targetId = 20;
        ChatRoom groupRoom = createRoom(2, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember ownerRequester = createRoomMember(groupRoom, createUser(requesterId, "방장", UserRole.USER), true,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember ownerTarget = createRoomMember(groupRoom, createUser(targetId, "대상 방장", UserRole.USER), true,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), requesterId))
            .willReturn(Optional.of(ownerRequester));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), targetId))
            .willReturn(Optional.of(ownerTarget));

        // when & then
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
    @DisplayName("toggleMute는 기존 setting이 false면 true로 토글한다")
    void toggleMuteTogglesFromUnmutedToMuted() {
        // given
        Integer userId = 10;
        Integer roomId = 1;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom room = createRoom(roomId, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(room, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        NotificationMuteSetting setting = NotificationMuteSetting.of(NotificationTargetType.CHAT_ROOM, roomId, user,
            false);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(NotificationTargetType.CHAT_ROOM,
            roomId, userId))
            .willReturn(Optional.of(setting));

        // when
        ChatMuteResponse response = chatService.toggleMute(userId, roomId);

        // then
        assertThat(response.isMuted()).isTrue();
        assertThat(setting.getIsMuted()).isTrue();
        verify(userRepository, times(1)).getById(userId);
        verify(notificationMuteSettingRepository).save(setting);
    }

    @Test
    @DisplayName("toggleMute는 기존 setting이 true면 false로 토글한다 (unmute)")
    void toggleMuteTogglesFromMutedToUnmuted() {
        // given
        Integer userId = 10;
        Integer roomId = 1;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom room = createRoom(roomId, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(room, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        NotificationMuteSetting setting = NotificationMuteSetting.of(NotificationTargetType.CHAT_ROOM, roomId, user,
            true);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(NotificationTargetType.CHAT_ROOM,
            roomId, userId))
            .willReturn(Optional.of(setting));

        // when
        ChatMuteResponse response = chatService.toggleMute(userId, roomId);

        // then
        assertThat(response.isMuted()).isFalse();
        assertThat(setting.getIsMuted()).isFalse();
        verify(notificationMuteSettingRepository).save(setting);
    }

    @Test
    @DisplayName("toggleMute는 기존 setting이 없으면 muted=true로 저장한다")
    void toggleMuteCreatesNewMutedSettingWhenNoneExists() {
        // given
        Integer userId = 10;
        Integer roomId = 1;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom room = createRoom(roomId, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(room, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));
        given(notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(NotificationTargetType.CHAT_ROOM,
            roomId, userId))
            .willReturn(Optional.empty());

        // when
        ChatMuteResponse response = chatService.toggleMute(userId, roomId);

        // then
        assertThat(response.isMuted()).isTrue();
        verify(notificationMuteSettingRepository).save(any(NotificationMuteSetting.class));
    }

    @Test
    @DisplayName("getMessages는 direct room에서 direct 전용 readAt 갱신 경로를 사용한다")
    void getMessagesUsesDirectReadPath() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember directMember = createRoomMember(directRoom, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage directMessage = createMessage(100, directRoom, createUser(20, "상대", UserRole.USER), "direct",
            LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomId(directRoom.getId())).willReturn(List.of(directMember));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(directRoom.getId(), userId)).willReturn(
            Optional.of(directMember));
        given(chatMessageRepository.findByChatRoomId(eq(directRoom.getId()), nullable(LocalDateTime.class),
            eq(PageRequest.of(0, 20))))
            .willReturn(new PageImpl<>(List.of(directMessage), PageRequest.of(0, 20), 1));

        // when
        ChatMessagePageResponse response = chatService.getMessages(userId, directRoom.getId(), 1, 20);

        // then
        assertThat(response.messages()).hasSize(1);
        verify(chatRoomMembershipService).updateDirectRoomLastReadAt(eq(directRoom.getId()), eq(user),
            any(LocalDateTime.class), eq(directRoom));
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
        given(chatMessageRepository.findByChatRoomId(eq(clubRoom.getId()), nullable(LocalDateTime.class),
            eq(PageRequest.of(0, 20))))
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
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId)).willReturn(
            Optional.of(groupMember));
        given(chatRoomMemberRepository.findByChatRoomId(groupRoom.getId())).willReturn(List.of(groupMember));
        given(chatMessageRepository.countByChatRoomId(groupRoom.getId(), null)).willReturn(1L);
        given(chatMessageRepository.findByChatRoomId(eq(groupRoom.getId()), nullable(LocalDateTime.class),
            eq(PageRequest.of(0, 20))))
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
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId)).willReturn(
            Optional.empty());

        // when & then
        assertErrorCode(
            () -> chatService.getMessages(userId, groupRoom.getId(), 1, 20),
            FORBIDDEN_CHAT_ROOM_ACCESS
        );
    }

    // ===== createOrGetChatRoom additional =====

    @Test
    @DisplayName("createOrGetChatRoom은 기존 방이 없으면 새 direct room을 생성한다")
    void createOrGetChatRoomCreatesNewDirectRoomWhenNoneExists() {
        // given
        Integer currentUserId = 10;
        Integer targetUserId = 20;
        User currentUser = createUser(currentUserId, "요청자", UserRole.USER);
        User targetUser = createUser(targetUserId, "상대", UserRole.USER);
        ChatRoom newRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(userRepository.getById(currentUserId)).willReturn(currentUser);
        given(userRepository.getById(targetUserId)).willReturn(targetUser);
        given(chatRoomRepository.findByTwoUsers(currentUserId, targetUserId, ChatType.DIRECT))
            .willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(newRoom);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(newRoom.getId(), currentUserId))
            .willReturn(Optional.empty());
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(newRoom.getId(), targetUserId))
            .willReturn(Optional.empty());

        // when
        ChatRoomResponse response = chatService.createOrGetChatRoom(currentUserId,
            new ChatRoomCreateRequest(targetUserId));

        // then
        assertThat(response.chatRoomId()).isEqualTo(newRoom.getId());
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("createOrGetChatRoom은 admin이 admin과 채팅할 때 일반 direct 경로를 사용한다")
    void createOrGetChatRoomTreatsAdminToAdminAsNormalDirect() {
        // given
        Integer adminId1 = 99;
        Integer adminId2 = 98;
        User admin1 = createUser(adminId1, "관리자1", UserRole.ADMIN);
        User admin2 = createUser(adminId2, "관리자2", UserRole.ADMIN);
        ChatRoom room = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member1 = createRoomMember(room, admin1, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member2 = createRoomMember(room, admin2, false, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(userRepository.getById(adminId1)).willReturn(admin1);
        given(userRepository.getById(adminId2)).willReturn(admin2);
        given(chatRoomRepository.findByTwoUsers(adminId1, adminId2, ChatType.DIRECT))
            .willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), adminId1))
            .willReturn(Optional.of(member1));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), adminId2))
            .willReturn(Optional.of(member2));

        // when
        ChatRoomResponse response = chatService.createOrGetChatRoom(adminId1, new ChatRoomCreateRequest(adminId2));

        // then
        assertThat(response.chatRoomId()).isEqualTo(room.getId());
        verify(chatRoomRepository, never()).findByTwoUsers(eq(SYSTEM_ADMIN_ID), any(), any());
    }

    // ===== createOrGetAdminChatRoom =====

    @Test
    @DisplayName("createOrGetAdminChatRoom은 admin이 없으면 NOT_FOUND_USER를 던진다")
    void createOrGetAdminChatRoomThrowsWhenNoAdminExists() {
        // given
        given(userRepository.findFirstByRoleAndDeletedAtIsNullOrderByIdAsc(UserRole.ADMIN))
            .willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.createOrGetAdminChatRoom(10), NOT_FOUND_USER);
    }

    // ===== leaveChatRoom additional =====

    @Test
    @DisplayName("leaveChatRoom은 존재하지 않는 방에 대해 NOT_FOUND_CHAT_ROOM을 던진다")
    void leaveChatRoomThrowsWhenRoomNotFound() {
        // given
        given(chatRoomRepository.findById(999)).willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.leaveChatRoom(10, 999), NOT_FOUND_CHAT_ROOM);
    }

    @Test
    @DisplayName("leaveChatRoom은 멤버가 아닌 사용자에 대해 FORBIDDEN_CHAT_ROOM_ACCESS를 던진다")
    void leaveChatRoomThrowsWhenNotMember() {
        // given
        Integer userId = 10;
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(directRoom.getId(), userId))
            .willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.leaveChatRoom(userId, directRoom.getId()), FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    // ===== kickMember additional =====

    @Test
    @DisplayName("kickMember는 존재하지 않는 방에 대해 NOT_FOUND_CHAT_ROOM을 던진다")
    void kickMemberThrowsWhenRoomNotFound() {
        // given
        given(chatRoomRepository.findById(999)).willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.kickMember(10, 999, 20), NOT_FOUND_CHAT_ROOM);
    }

    @Test
    @DisplayName("kickMember는 club group room에서도 거부한다")
    void kickMemberRejectsClubGroupRoom() {
        // given
        ChatRoom clubRoom = createRoom(1, ChatType.CLUB_GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        given(chatRoomRepository.findById(clubRoom.getId())).willReturn(Optional.of(clubRoom));

        // when & then
        assertErrorCode(() -> chatService.kickMember(10, clubRoom.getId(), 20), CANNOT_KICK_IN_NON_GROUP_ROOM);
    }

    @Test
    @DisplayName("kickMember는 요청자가 멤버가 아니면 FORBIDDEN_CHAT_ROOM_ACCESS를 던진다")
    void kickMemberThrowsWhenRequesterNotMember() {
        // given
        Integer requesterId = 10;
        Integer targetId = 20;
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), requesterId))
            .willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.kickMember(requesterId, groupRoom.getId(), targetId),
            FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    @Test
    @DisplayName("kickMember는 target이 멤버가 아니면 FORBIDDEN_CHAT_ROOM_ACCESS를 던진다")
    void kickMemberThrowsWhenTargetNotMember() {
        // given
        Integer requesterId = 10;
        Integer targetId = 20;
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember owner = createRoomMember(groupRoom, createUser(requesterId, "방장", UserRole.USER), true,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), requesterId))
            .willReturn(Optional.of(owner));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), targetId))
            .willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.kickMember(requesterId, groupRoom.getId(), targetId),
            FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    // ===== getMessages additional =====

    @Test
    @DisplayName("getMessages는 존재하지 않는 방에 대해 NOT_FOUND_CHAT_ROOM을 던진다")
    void getMessagesThrowsWhenRoomNotFound() {
        // given
        given(chatRoomRepository.findById(999)).willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.getMessages(10, 999, 1, 20), NOT_FOUND_CHAT_ROOM);
    }

    @Test
    @DisplayName("getMessages는 admin이 system admin 방을 조회할 때 전용 경로를 사용한다")
    void getMessagesReturnsAdminSystemRoomMessages() {
        // given
        Integer adminId = 99;
        User admin = createUser(adminId, "관리자", UserRole.ADMIN);
        User systemAdmin = createUser(SYSTEM_ADMIN_ID, "시스템관리자", UserRole.ADMIN);
        User targetUser = createUser(20, "사용자", UserRole.USER);
        ChatRoom systemAdminRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember systemAdminMember = createRoomMember(systemAdminRoom, systemAdmin, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember targetMember = createRoomMember(systemAdminRoom, targetUser, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ReflectionTestUtils.setField(systemAdminMember, "visibleMessageFrom",
            LocalDateTime.of(2026, 4, 11, 10, 5));
        ReflectionTestUtils.setField(targetMember, "visibleMessageFrom",
            LocalDateTime.of(2026, 4, 11, 10, 7));
        systemAdminMember.updateLastReadAt(LocalDateTime.of(2026, 4, 11, 10, 7));
        targetMember.updateLastReadAt(LocalDateTime.of(2026, 4, 11, 10, 3));
        ChatMessage adminMessage = createMessage(100, systemAdminRoom, admin, "관리자 답변",
            LocalDateTime.of(2026, 4, 11, 10, 6));
        ChatMessage userMessage = createMessage(101, systemAdminRoom, targetUser, "사용자 문의",
            LocalDateTime.of(2026, 4, 11, 10, 8));

        given(chatRoomRepository.findById(systemAdminRoom.getId())).willReturn(Optional.of(systemAdminRoom));
        given(userRepository.getById(adminId)).willReturn(admin);
        given(chatRoomSystemAdminService.isSystemAdminRoom(systemAdminRoom.getId())).willReturn(true);
        given(chatRoomSystemAdminService.findSystemAdminMember(List.of(systemAdminMember, targetMember)))
            .willReturn(systemAdminMember);
        given(chatRoomMemberRepository.findByChatRoomId(systemAdminRoom.getId()))
            .willReturn(List.of(systemAdminMember, targetMember));
        given(chatMessageRepository.findByChatRoomId(eq(systemAdminRoom.getId()),
            eq(systemAdminMember.getVisibleMessageFrom()),
            eq(PageRequest.of(0, 20))))
            .willReturn(new PageImpl<>(List.of(adminMessage, userMessage), PageRequest.of(0, 20), 2));

        // when
        ChatMessagePageResponse response = chatService.getMessages(adminId, systemAdminRoom.getId(), 1, 20);

        // then
        assertThat(response.messages())
            .extracting(
                ChatMessageDetailResponse::senderId,
                ChatMessageDetailResponse::content,
                ChatMessageDetailResponse::unreadCount,
                ChatMessageDetailResponse::isMine
            )
            .containsExactly(
                tuple(adminId, "관리자 답변", 0, true),
                tuple(targetUser.getId(), "사용자 문의", 2, false)
            );
        verify(chatRoomMembershipService).updateLastReadAt(eq(systemAdminRoom.getId()), eq(SYSTEM_ADMIN_ID),
            any(LocalDateTime.class));
        verify(chatPresenceService).recordPresence(systemAdminRoom.getId(), adminId);
    }

    @Test
    @DisplayName("getMessages는 일반 사용자의 system admin 방 조회에서 가시 범위와 sender masking을 적용한다")
    void getMessagesAppliesVisibilityAndSenderMaskingForUserInSystemAdminRoom() {
        // given
        Integer userId = 20;
        Integer adminId = 99;
        User user = createUser(userId, "사용자", UserRole.USER);
        User admin = createUser(adminId, "관리자", UserRole.ADMIN);
        User systemAdmin = createUser(SYSTEM_ADMIN_ID, "시스템관리자", UserRole.ADMIN);
        ChatRoom systemAdminRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember userMember = createRoomMember(systemAdminRoom, user, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember systemAdminMember = createRoomMember(systemAdminRoom, systemAdmin, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ReflectionTestUtils.setField(userMember, "visibleMessageFrom", LocalDateTime.of(2026, 4, 11, 10, 5));
        ReflectionTestUtils.setField(systemAdminMember, "visibleMessageFrom",
            LocalDateTime.of(2026, 4, 11, 10, 7));
        userMember.updateLastReadAt(LocalDateTime.of(2026, 4, 11, 10, 7));
        systemAdminMember.updateLastReadAt(LocalDateTime.of(2026, 4, 11, 10, 3));
        ChatMessage adminMessage = createMessage(100, systemAdminRoom, admin, "관리자 답변",
            LocalDateTime.of(2026, 4, 11, 10, 6));
        ChatMessage userMessage = createMessage(101, systemAdminRoom, user, "사용자 문의",
            LocalDateTime.of(2026, 4, 11, 10, 8));

        given(chatRoomRepository.findById(systemAdminRoom.getId())).willReturn(Optional.of(systemAdminRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(systemAdminRoom.getId(), userId))
            .willReturn(Optional.of(userMember));
        given(chatRoomMemberRepository.findByChatRoomId(systemAdminRoom.getId()))
            .willReturn(List.of(systemAdminMember, userMember));
        given(chatMessageRepository.findByChatRoomId(eq(systemAdminRoom.getId()),
            eq(userMember.getVisibleMessageFrom()),
            eq(PageRequest.of(0, 20))))
            .willReturn(new PageImpl<>(List.of(adminMessage, userMessage), PageRequest.of(0, 20), 2));

        // when
        ChatMessagePageResponse response = chatService.getMessages(userId, systemAdminRoom.getId(), 1, 20);

        // then
        assertThat(response.messages())
            .extracting(
                ChatMessageDetailResponse::senderId,
                ChatMessageDetailResponse::content,
                ChatMessageDetailResponse::unreadCount,
                ChatMessageDetailResponse::isMine
            )
            .containsExactly(
                tuple(SYSTEM_ADMIN_ID, "관리자 답변", 0, false),
                tuple(userId, "사용자 문의", 2, true)
            );
        verify(chatRoomMembershipService).updateDirectRoomLastReadAt(eq(systemAdminRoom.getId()), eq(user),
            any(LocalDateTime.class), eq(systemAdminRoom));
        verify(chatPresenceService).recordPresence(systemAdminRoom.getId(), userId);
    }

    // ===== sendMessage =====

    @Test
    @DisplayName("sendMessage는 존재하지 않는 방에 대해 NOT_FOUND_CHAT_ROOM을 던진다")
    void sendMessageThrowsWhenRoomNotFound() {
        // given
        given(chatRoomRepository.findById(999)).willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.sendMessage(10, 999, new ChatMessageSendRequest("hi")), NOT_FOUND_CHAT_ROOM);
    }

    @Test
    @DisplayName("sendMessage는 direct room에서 메시지를 저장하고 알림을 보낸다")
    void sendMessageInDirectRoomSavesMessageAndSendsNotification() {
        // given
        Integer senderId = 10;
        Integer receiverId = 20;
        User sender = createUser(senderId, "보낸이", UserRole.USER);
        User receiver = createUser(receiverId, "받는이", UserRole.USER);
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember senderMember = createRoomMember(directRoom, sender, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember receiverMember = createRoomMember(directRoom, receiver, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage savedMessage = createMessage(100, directRoom, sender, "hello",
            LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(userRepository.getById(senderId)).willReturn(sender);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(directRoom.getId(), senderId))
            .willReturn(Optional.of(senderMember));
        given(chatRoomMemberRepository.findByChatRoomId(directRoom.getId()))
            .willReturn(List.of(senderMember, receiverMember));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);
        given(chatRoomRepository.updateLastMessageIfLatest(
            directRoom.getId(), savedMessage.getId(), savedMessage.getContent(), savedMessage.getCreatedAt()
        )).willReturn(1);
        given(chatRoomMemberRepository.updateLastReadAtIfOlder(eq(directRoom.getId()), eq(senderId),
            any(LocalDateTime.class)))
            .willReturn(1);

        // when
        ChatMessageDetailResponse response = chatService.sendMessage(senderId, directRoom.getId(),
            new ChatMessageSendRequest("hello"));

        // then
        assertThat(response.messageId()).isEqualTo(savedMessage.getId());
        assertThat(response.content()).isEqualTo("hello");
        assertThat(response.senderId()).isEqualTo(senderId);
        assertThat(response.isMine()).isTrue();
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(notificationService).sendChatNotification(eq(receiverId), eq(directRoom.getId()), eq("보낸이"),
            eq("hello"));
    }

    @Test
    @DisplayName("sendMessage는 이미 더 최신 메시지가 있으면 room 마지막 메시지 메타데이터를 덮어쓰지 않는다")
    void sendMessageDoesNotOverwriteRoomMetadataWhenNewerMessageAlreadyExists() {
        Integer senderId = 10;
        Integer receiverId = 20;
        User sender = createUser(senderId, "보낸이", UserRole.USER);
        User receiver = createUser(receiverId, "받는이", UserRole.USER);
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember senderMember = createRoomMember(directRoom, sender, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember receiverMember = createRoomMember(directRoom, receiver, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage savedMessage = createMessage(100, directRoom, sender, "older",
            LocalDateTime.of(2026, 4, 11, 10, 1));

        ReflectionTestUtils.setField(directRoom, "lastMessageContent", "newer");
        ReflectionTestUtils.setField(directRoom, "lastMessageSentAt", LocalDateTime.of(2026, 4, 11, 10, 2));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(userRepository.getById(senderId)).willReturn(sender);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(directRoom.getId(), senderId))
            .willReturn(Optional.of(senderMember));
        given(chatRoomMemberRepository.findByChatRoomId(directRoom.getId()))
            .willReturn(List.of(senderMember, receiverMember));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);
        given(chatRoomRepository.updateLastMessageIfLatest(
            directRoom.getId(), savedMessage.getId(), savedMessage.getContent(), savedMessage.getCreatedAt()
        )).willReturn(0);
        given(chatRoomMemberRepository.updateLastReadAtIfOlder(eq(directRoom.getId()), eq(senderId),
            any(LocalDateTime.class)))
            .willReturn(1);

        chatService.sendMessage(senderId, directRoom.getId(), new ChatMessageSendRequest("older"));

        assertThat(directRoom.getLastMessageContent()).isEqualTo("newer");
        assertThat(directRoom.getLastMessageSentAt()).isEqualTo(LocalDateTime.of(2026, 4, 11, 10, 2));
    }

    @Test
    @DisplayName("sendMessage는 group room에서 메시지를 저장하고 그룹 알림을 보낸다")
    void sendMessageInGroupRoomSavesMessageAndSendsGroupNotification() {
        // given
        Integer senderId = 10;
        User sender = createUser(senderId, "보낸이", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember senderMember = createRoomMember(groupRoom, sender, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage savedMessage = createMessage(100, groupRoom, sender, "hello",
            LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(senderId)).willReturn(sender);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), senderId))
            .willReturn(Optional.of(senderMember));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);
        given(chatRoomRepository.updateLastMessageIfLatest(
            groupRoom.getId(), savedMessage.getId(), savedMessage.getContent(), savedMessage.getCreatedAt()
        )).willReturn(1);
        given(chatRoomMemberRepository.updateLastReadAtIfOlder(eq(groupRoom.getId()), eq(senderId),
            any(LocalDateTime.class)))
            .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(groupRoom.getId()))
            .willReturn(List.of(senderMember));

        // when
        ChatMessageDetailResponse response = chatService.sendMessage(senderId, groupRoom.getId(),
            new ChatMessageSendRequest("hello"));

        // then
        assertThat(response.messageId()).isEqualTo(savedMessage.getId());
        assertThat(response.content()).isEqualTo("hello");
        assertThat(response.isMine()).isTrue();
        verify(notificationService).sendGroupChatNotification(
            eq(groupRoom.getId()), eq(senderId), eq("그룹 채팅"), eq("보낸이"), eq("hello"),
            any(List.class)
        );
    }

    @Test
    @DisplayName("sendMessage는 group room 멤버의 hasLeft 상태면 FORBIDDEN_CHAT_ROOM_ACCESS를 던진다")
    void sendMessageInGroupRoomRejectsLeftMember() {
        // given
        Integer senderId = 10;
        User sender = createUser(senderId, "보낸이", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember leftMember = createRoomMember(groupRoom, sender, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        markMemberLeft(leftMember, LocalDateTime.of(2026, 4, 11, 12, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(senderId)).willReturn(sender);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), senderId))
            .willReturn(Optional.of(leftMember));

        // when & then
        assertErrorCode(
            () -> chatService.sendMessage(senderId, groupRoom.getId(), new ChatMessageSendRequest("hello")),
            FORBIDDEN_CHAT_ROOM_ACCESS
        );
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("sendMessage는 club room에서 메시지를 저장하고 그룹 알림을 보낸다")
    void sendMessageInClubRoomSavesMessageAndSendsGroupNotification() {
        // given
        Integer senderId = 10;
        User sender = createUser(senderId, "보낸이", UserRole.USER);
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 77, "BCSD");
        ChatRoom clubRoom = ChatRoom.clubGroupOf(club);
        ReflectionTestUtils.setField(clubRoom, "id", 1);
        ReflectionTestUtils.setField(clubRoom, "createdAt", LocalDateTime.of(2026, 4, 11, 10, 0));
        ClubMember clubMember = ClubMemberFixture.createMember(club, sender);
        ReflectionTestUtils.setField(clubMember, "createdAt", LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember senderRoomMember = createRoomMember(clubRoom, sender, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage savedMessage = createMessage(100, clubRoom, sender, "hello",
            LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findById(clubRoom.getId())).willReturn(Optional.of(clubRoom));
        given(clubMemberRepository.getByClubIdAndUserId(club.getId(), senderId)).willReturn(clubMember);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(clubRoom.getId(), senderId))
            .willReturn(Optional.of(senderRoomMember));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);
        given(chatRoomRepository.updateLastMessageIfLatest(
            clubRoom.getId(), savedMessage.getId(), savedMessage.getContent(), savedMessage.getCreatedAt()
        )).willReturn(1);
        given(chatRoomMemberRepository.updateLastReadAtIfOlder(eq(clubRoom.getId()), eq(senderId),
            any(LocalDateTime.class)))
            .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(clubRoom.getId()))
            .willReturn(List.of(senderRoomMember));

        // when
        ChatMessageDetailResponse response = chatService.sendMessage(senderId, clubRoom.getId(),
            new ChatMessageSendRequest("hello"));

        // then
        assertThat(response.messageId()).isEqualTo(savedMessage.getId());
        assertThat(response.content()).isEqualTo("hello");
        verify(notificationService).sendGroupChatNotification(
            eq(clubRoom.getId()), eq(senderId), eq("BCSD"), eq("보낸이"), eq("hello"),
            any(List.class)
        );
    }

    @Test
    @DisplayName("sendMessage는 일반 사용자가 SYSTEM_ADMIN 방에 보내면 관리자 문의 이벤트를 발행한다")
    void sendMessageByUserInSystemAdminRoomPublishesAdminChatEvent() {
        // given
        Integer senderId = 20;
        String content = "문의합니다";
        User sender = createUser(senderId, "사용자", UserRole.USER);
        User systemAdmin = createUser(SYSTEM_ADMIN_ID, "시스템관리자", UserRole.ADMIN);
        ChatRoom systemAdminRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember senderMember = createRoomMember(systemAdminRoom, sender, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember systemAdminMember = createRoomMember(systemAdminRoom, systemAdmin, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage savedMessage = createMessage(100, systemAdminRoom, sender, content,
            LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findById(systemAdminRoom.getId())).willReturn(Optional.of(systemAdminRoom));
        given(userRepository.getById(senderId)).willReturn(sender);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(systemAdminRoom.getId(), senderId))
            .willReturn(Optional.of(senderMember));
        given(chatRoomMemberRepository.findByChatRoomId(systemAdminRoom.getId()))
            .willReturn(List.of(systemAdminMember, senderMember));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);
        given(chatRoomRepository.updateLastMessageIfLatest(
            systemAdminRoom.getId(), savedMessage.getId(), savedMessage.getContent(), savedMessage.getCreatedAt()
        )).willReturn(1);
        given(chatRoomMemberRepository.updateLastReadAtIfOlder(eq(systemAdminRoom.getId()), eq(senderId),
            any(LocalDateTime.class)))
            .willReturn(1);

        // when
        chatService.sendMessage(senderId, systemAdminRoom.getId(), new ChatMessageSendRequest(content));

        // then
        ArgumentCaptor<AdminChatReceivedEvent> eventCaptor = ArgumentCaptor.forClass(AdminChatReceivedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
            .extracting(
                AdminChatReceivedEvent::senderId,
                AdminChatReceivedEvent::senderName,
                AdminChatReceivedEvent::content
            )
            .containsExactly(senderId, sender.getName(), content);
    }

    @Test
    @DisplayName("sendMessage는 admin이 system admin room에 보내면 멤버십 체크를 건너뛰고 lastReadAt 업데이트도 하지 않는다")
    void sendMessageAdminBypassesMembershipInSystemAdminRoom() {
        // given
        Integer adminId = 99;
        Integer targetUserId = 20;
        User admin = createUser(adminId, "관리자", UserRole.ADMIN);
        User systemAdmin = createUser(SYSTEM_ADMIN_ID, "시스템관리자", UserRole.ADMIN);
        User targetUser = createUser(targetUserId, "사용자", UserRole.USER);
        ChatRoom systemAdminRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember systemAdminMember = createRoomMember(systemAdminRoom, systemAdmin, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember targetMember = createRoomMember(systemAdminRoom, targetUser, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage savedMessage = createMessage(100, systemAdminRoom, admin, "문의",
            LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findById(systemAdminRoom.getId())).willReturn(Optional.of(systemAdminRoom));
        given(userRepository.getById(adminId)).willReturn(admin);
        given(chatRoomSystemAdminService.isSystemAdminRoom(systemAdminRoom.getId())).willReturn(true);
        given(chatRoomMemberRepository.findByChatRoomId(systemAdminRoom.getId()))
            .willReturn(List.of(systemAdminMember, targetMember));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);
        given(chatRoomRepository.updateLastMessageIfLatest(
            systemAdminRoom.getId(), savedMessage.getId(), savedMessage.getContent(), savedMessage.getCreatedAt()
        )).willReturn(1);

        // when
        ChatMessageDetailResponse response = chatService.sendMessage(adminId, systemAdminRoom.getId(),
            new ChatMessageSendRequest("문의"));

        // then
        assertThat(response.content()).isEqualTo("문의");
        assertThat(response.isMine()).isTrue();
        // 멤버십 조회를 건너뛰어야 한다
        verify(chatRoomMemberRepository, never()).findByChatRoomIdAndUserId(systemAdminRoom.getId(), adminId);
        // admin은 lastReadAt 업데이트를 하지 않는다
        verify(chatRoomMemberRepository, never()).updateLastReadAtIfOlder(eq(systemAdminRoom.getId()), eq(adminId),
            any(LocalDateTime.class));
        // 비관리자에게 알림이 전송되어야 한다
        verify(notificationService).sendChatNotification(eq(targetUserId), eq(systemAdminRoom.getId()), eq("관리자"),
            eq("문의"));
        verify(eventPublisher, never()).publishEvent(any(AdminChatReceivedEvent.class));
    }

    // ===== toggleMute additional =====

    @Test
    @DisplayName("toggleMute는 존재하지 않는 방에 대해 NOT_FOUND_CHAT_ROOM을 던진다")
    void toggleMuteThrowsWhenRoomNotFound() {
        // given
        given(chatRoomRepository.findById(999)).willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.toggleMute(10, 999), NOT_FOUND_CHAT_ROOM);
    }

    @Test
    @DisplayName("toggleMute는 group room에서 멤버가 아니면 FORBIDDEN_CHAT_ROOM_ACCESS를 던진다")
    void toggleMuteRejectsNonMemberInGroupRoom() {
        // given
        Integer userId = 10;
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(userId)).willReturn(createUser(userId, "사용자", UserRole.USER));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId))
            .willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> chatService.toggleMute(userId, groupRoom.getId()), FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    // ===== updateChatRoomName =====

    @Test
    @DisplayName("updateChatRoomName은 멤버의 custom room name을 업데이트한다")
    void updateChatRoomNameUpdatesCustomNameForMember() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(groupRoom, user, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId))
            .willReturn(Optional.of(member));

        // when
        chatService.updateChatRoomName(userId, groupRoom.getId(), new ChatRoomNameUpdateRequest("내 채팅방"));

        // then
        assertThat(member.getCustomRoomName()).isEqualTo("내 채팅방");
    }

    @Test
    @DisplayName("updateChatRoomName은 멤버가 아니면 FORBIDDEN_CHAT_ROOM_ACCESS를 던진다")
    void updateChatRoomNameRejectsNonMember() {
        // given
        Integer userId = 10;
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId))
            .willReturn(Optional.empty());

        // when & then
        assertErrorCode(
            () -> chatService.updateChatRoomName(userId, groupRoom.getId(), new ChatRoomNameUpdateRequest("이름")),
            FORBIDDEN_CHAT_ROOM_ACCESS
        );
    }

    @Test
    @DisplayName("updateChatRoomName은 null이나 빈 이름을 null로 정규화한다")
    void updateChatRoomNameNormalizesNullName() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(groupRoom, user, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        member.updateCustomRoomName("기존 이름");

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId))
            .willReturn(Optional.of(member));

        // when
        chatService.updateChatRoomName(userId, groupRoom.getId(), new ChatRoomNameUpdateRequest(null));

        // then
        assertThat(member.getCustomRoomName()).isNull();
    }

    // ===== getMessages with messageId (US-003) =====

    @Test
    @DisplayName("getMessages는 messageId가 null이면 기존 동작과 동일하게 동작한다")
    void getMessagesWithNullMessageIdBehavesIdentically() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom groupRoom = createRoom(3, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember groupMember = createRoomMember(groupRoom, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatMessage groupMessage = createMessage(300, groupRoom, user, "group", LocalDateTime.of(2026, 4, 11, 10, 3));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId)).willReturn(
            Optional.of(groupMember));
        given(chatRoomMemberRepository.findByChatRoomId(groupRoom.getId())).willReturn(List.of(groupMember));
        given(chatMessageRepository.countByChatRoomId(groupRoom.getId(), null)).willReturn(1L);
        given(chatMessageRepository.findByChatRoomId(eq(groupRoom.getId()), nullable(LocalDateTime.class),
            eq(PageRequest.of(0, 20))))
            .willReturn(new PageImpl<>(List.of(groupMessage), PageRequest.of(0, 20), 1));

        // when — 기존 4-arg 오버로드 호출
        ChatMessagePageResponse response = chatService.getMessages(userId, groupRoom.getId(), 1, 20);

        // then
        assertThat(response.messages()).hasSize(1);
        verify(chatMessageRepository, never()).findByIdWithChatRoom(any());
        verify(chatMessageRepository, never()).countNewerMessagesByChatRoomId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getMessages는 존재하지 않는 messageId에 대해 NOT_FOUND_CHAT_ROOM을 던진다")
    void getMessagesWithMessageIdThrowsWhenMessageNotFound() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember groupMember = createRoomMember(groupRoom, user, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId))
            .willReturn(Optional.of(groupMember));
        given(chatMessageRepository.findByIdWithChatRoom(999)).willReturn(Optional.empty());

        // when & then
        assertErrorCode(
            () -> chatService.getMessages(userId, groupRoom.getId(), 1, 20, 999),
            NOT_FOUND_CHAT_ROOM
        );
    }

    @Test
    @DisplayName("getMessages는 다른 채팅방의 messageId에 대해 NOT_FOUND_CHAT_ROOM을 던진다")
    void getMessagesWithMessageIdThrowsWhenMessageBelongsToOtherRoom() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoom otherRoom = createRoom(2, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember groupMember = createRoomMember(groupRoom, user, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        User sender = createUser(20, "작성자", UserRole.USER);
        ChatMessage otherRoomMessage = createMessage(100, otherRoom, sender, "다른 방 메시지",
            LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId))
            .willReturn(Optional.of(groupMember));
        given(chatMessageRepository.findByIdWithChatRoom(100)).willReturn(Optional.of(otherRoomMessage));

        // when & then
        assertErrorCode(
            () -> chatService.getMessages(userId, groupRoom.getId(), 1, 20, 100),
            NOT_FOUND_CHAT_ROOM
        );
    }

    @Test
    @DisplayName("getMessages는 group room에서 올바른 messageId 제공 시 계산된 페이지를 반환한다")
    void getMessagesWithMessageIdCalculatesCorrectPageInGroupRoom() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember groupMember = createRoomMember(groupRoom, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));

        // 타겟 메시지: roomId=1, id=50, createdAt=14:00
        ChatMessage targetMessage = createMessage(50, groupRoom, user, "찾는 메시지",
            LocalDateTime.of(2026, 4, 11, 14, 0));

        // 타겟 메시지보다 최신인 메시지가 25개 → page = 25/20 + 1 = 2
        ChatMessage page2Message = createMessage(30, groupRoom, user, "페이지2 메시지",
            LocalDateTime.of(2026, 4, 11, 13, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatMessageRepository.findByIdWithChatRoom(50)).willReturn(Optional.of(targetMessage));
        given(chatMessageRepository.countNewerMessagesByChatRoomId(
            groupRoom.getId(), 50, targetMessage.getCreatedAt(), null))
            .willReturn(25L);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId)).willReturn(
            Optional.of(groupMember));
        given(chatRoomMemberRepository.findByChatRoomId(groupRoom.getId())).willReturn(List.of(groupMember));
        given(chatMessageRepository.countByChatRoomId(groupRoom.getId(), null)).willReturn(100L);
        given(chatMessageRepository.findByChatRoomId(eq(groupRoom.getId()), nullable(LocalDateTime.class),
            eq(PageRequest.of(1, 20))))  // page=2이므로 offset=20
            .willReturn(new PageImpl<>(List.of(page2Message, targetMessage), PageRequest.of(1, 20), 100L));

        // when — page=1을 보내도 서버가 page=2로 덮어씀
        ChatMessagePageResponse response = chatService.getMessages(userId, groupRoom.getId(), 1, 20, 50);

        // then
        assertThat(response.currentPage()).isEqualTo(2);
        assertThat(response.messages().stream().anyMatch(m -> m.messageId().equals(50))).isTrue();
        verify(chatMessageRepository).countNewerMessagesByChatRoomId(
            groupRoom.getId(), 50, targetMessage.getCreatedAt(), null);
    }

    @Test
    @DisplayName("getMessages는 visibleMessageFrom 범위 밖 messageId에 대해 NOT_FOUND_CHAT_ROOM을 던진다")
    void getMessagesWithMessageIdThrowsWhenMessageBeforeVisibleMessageFrom() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(directRoom, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));
        // 사용자가 나갔다가 돌아옴 → visibleMessageFrom이 설정됨
        markMemberLeft(member, LocalDateTime.of(2026, 4, 11, 12, 0));

        // 타겟 메시지가 visibleMessageFrom(12:00)보다 이전(10:30)에 작성됨
        User partner = createUser(20, "상대", UserRole.USER);
        ChatMessage oldMessage = createMessage(50, directRoom, partner, "오래된 메시지",
            LocalDateTime.of(2026, 4, 11, 10, 30));

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatMessageRepository.findByIdWithChatRoom(50)).willReturn(Optional.of(oldMessage));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(directRoom.getId(), userId))
            .willReturn(Optional.of(member));

        // when & then
        assertErrorCode(
            () -> chatService.getMessages(userId, directRoom.getId(), 1, 20, 50),
            NOT_FOUND_CHAT_ROOM
        );
    }

    @Test
    @DisplayName("getMessages는 messageId가 최신 메시지면 page=1을 계산한다")
    void getMessagesWithMessageIdReturnsPage1ForNewestMessage() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember groupMember = createRoomMember(groupRoom, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));

        ChatMessage newestMessage = createMessage(100, groupRoom, user, "최신 메시지",
            LocalDateTime.of(2026, 4, 11, 15, 0));

        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatMessageRepository.findByIdWithChatRoom(100)).willReturn(Optional.of(newestMessage));
        // 최신 메시지보다 더 최신인 메시지가 0개 → page = 0/20 + 1 = 1
        given(chatMessageRepository.countNewerMessagesByChatRoomId(
            groupRoom.getId(), 100, newestMessage.getCreatedAt(), null))
            .willReturn(0L);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), userId)).willReturn(
            Optional.of(groupMember));
        given(chatRoomMemberRepository.findByChatRoomId(groupRoom.getId())).willReturn(List.of(groupMember));
        given(chatMessageRepository.countByChatRoomId(groupRoom.getId(), null)).willReturn(50L);
        given(chatMessageRepository.findByChatRoomId(eq(groupRoom.getId()), nullable(LocalDateTime.class),
            eq(PageRequest.of(0, 20))))
            .willReturn(new PageImpl<>(List.of(newestMessage), PageRequest.of(0, 20), 50L));

        // when
        ChatMessagePageResponse response = chatService.getMessages(userId, groupRoom.getId(), 1, 20, 100);

        // then
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.messages().stream().anyMatch(m -> m.messageId().equals(100))).isTrue();
    }

    @Test
    @DisplayName("getMessages는 비회원이 messageId로 조회하면 NOT_FOUND_CHAT_ROOM을 던진다 (오라클 방지)")
    void getMessagesWithMessageIdRejectsNonMemberWithNotFound() {
        // given
        Integer nonMemberId = 99;
        User nonMember = createUser(nonMemberId, "비회원", UserRole.USER);
        ChatRoom groupRoom = createRoom(1, ChatType.GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        // messageId=50에 해당하는 메시지가 존재하더라도 비회원이므로 404
        given(chatRoomRepository.findById(groupRoom.getId())).willReturn(Optional.of(groupRoom));
        given(userRepository.getById(nonMemberId)).willReturn(nonMember);
        // 비회원은 멤버십이 없음
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(groupRoom.getId(), nonMemberId))
            .willReturn(Optional.empty());

        // when & then — 유효한 messageId여도 접근 권한 없음과 동일한 404
        assertErrorCode(
            () -> chatService.getMessages(nonMemberId, groupRoom.getId(), 1, 20, 50),
            NOT_FOUND_CHAT_ROOM
        );
        // messageId 조회 자체가 실행되지 않아야 함
        verify(chatMessageRepository, never()).findByIdWithChatRoom(any());
    }

    @Test
    @DisplayName("getMessages는 visibleMessageFrom과 동일 시각의 messageId를 거부한다 (경계 조건)")
    void getMessagesWithMessageIdRejectsMessageAtExactVisibleMessageFromBoundary() {
        // given
        Integer userId = 10;
        User user = createUser(userId, "사용자", UserRole.USER);
        ChatRoom directRoom = createRoom(1, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember member = createRoomMember(directRoom, user, false,
            LocalDateTime.of(2026, 4, 11, 10, 0));
        LocalDateTime leftAt = LocalDateTime.of(2026, 4, 11, 12, 0);
        markMemberLeft(member, leftAt);

        // 메시지가 visibleMessageFrom과 정확히 같은 시각
        User partner = createUser(20, "상대", UserRole.USER);
        ChatMessage boundaryMessage = createMessage(50, directRoom, partner, "경계 메시지", leftAt);

        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));
        given(userRepository.getById(userId)).willReturn(user);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(directRoom.getId(), userId))
            .willReturn(Optional.of(member));
        given(chatMessageRepository.findByIdWithChatRoom(50)).willReturn(Optional.of(boundaryMessage));

        // when & then
        assertErrorCode(
            () -> chatService.getMessages(userId, directRoom.getId(), 1, 20, 50),
            NOT_FOUND_CHAT_ROOM
        );
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

    private ChatRoom createClubRoom(Integer id, LocalDateTime createdAt) {
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 77, "BCSD");
        ChatRoom room = ChatRoom.clubGroupOf(club);
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", createdAt);
        return room;
    }

    private ChatRoomMember createRoomMember(ChatRoom room, User user, boolean isOwner, LocalDateTime lastReadAt) {
        ChatRoomMember member =
            isOwner ? ChatRoomMember.ofOwner(room, user, lastReadAt) : ChatRoomMember.of(room, user, lastReadAt);
        ReflectionTestUtils.setField(member, "createdAt", lastReadAt);
        return member;
    }

    private ChatMessage createMessage(Integer id, ChatRoom room, User sender, String content, LocalDateTime createdAt) {
        ChatMessage message = ChatMessage.of(room, sender, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        return message;
    }

    private void markMemberLeft(ChatRoomMember member, LocalDateTime leftAt) {
        ReflectionTestUtils.setField(member, "leftAt", leftAt);
        ReflectionTestUtils.setField(member, "visibleMessageFrom", leftAt);
    }

    private void assertErrorCode(ThrowingCallable callable, ApiResponseCode errorCode) {
        assertThatThrownBy(callable)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(errorCode));
    }
}
