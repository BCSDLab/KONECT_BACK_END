package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.chat.dto.AdminChatRoomProjection;
import gg.agit.konect.domain.chat.dto.ChatInvitableUsersResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMuteResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomNameUpdateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomSummaryResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsSummaryResponse;
import gg.agit.konect.domain.chat.dto.UnreadMessageCount;
import gg.agit.konect.domain.chat.enums.ChatInviteSortBy;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.event.AdminChatReceivedEvent;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatInviteQueryRepository;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.repository.RoomUnreadCountProjection;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int SYSTEM_ADMIN_ID = 1;
    private static final String ETC_SECTION_NAME = "기타";
    private static final String DEFAULT_GROUP_ROOM_NAME = "그룹 채팅";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ChatInviteQueryRepository chatInviteQueryRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final ChatRoomMembershipService chatRoomMembershipService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Integer currentUserId, ChatRoomCreateRequest request) {
        User currentUser = userRepository.getById(currentUserId);
        User targetUser = userRepository.getById(request.userId());

        if (currentUser.getId().equals(targetUser.getId())) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }

        if (currentUser.getRole() == UserRole.ADMIN && targetUser.getRole() != UserRole.ADMIN) {
            return getOrCreateSystemAdminChatRoomForUser(targetUser, currentUser);
        }

        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(
                currentUser.getId(),
                targetUser.getId(),
                ChatType.DIRECT
            )
            .orElseGet(() -> chatRoomRepository.save(ChatRoom.directOf()));

        LocalDateTime joinedAt = Objects.requireNonNull(chatRoom.getCreatedAt(), "chatRoom.createdAt must not be null");
        ensureDirectRoomRequester(chatRoom, currentUser, joinedAt);
        ensureRoomMember(chatRoom, targetUser, joinedAt);

        return ChatRoomResponse.from(chatRoom);
    }

    private ChatRoomResponse getOrCreateSystemAdminChatRoomForUser(User targetUser, User adminUser) {
        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(SYSTEM_ADMIN_ID, targetUser.getId(), ChatType.DIRECT)
            .orElseGet(() -> {
                ChatRoom newRoom = chatRoomRepository.save(ChatRoom.directOf());
                User systemAdmin = userRepository.getById(SYSTEM_ADMIN_ID);
                LocalDateTime joinedAt = Objects.requireNonNull(
                    newRoom.getCreatedAt(), "chatRoom.createdAt must not be null"
                );
                ensureRoomMember(newRoom, systemAdmin, joinedAt);
                ensureRoomMember(newRoom, targetUser, joinedAt);
                return newRoom;
            });

        LocalDateTime joinedAt = Objects.requireNonNull(
            chatRoom.getCreatedAt(), "chatRoom.createdAt must not be null"
        );
        ensureDirectRoomRequester(chatRoom, adminUser, joinedAt);

        return ChatRoomResponse.from(chatRoom);
    }

    @Transactional
    public ChatRoomResponse createOrGetAdminChatRoom(Integer currentUserId) {
        User adminUser = userRepository.findFirstByRoleAndDeletedAtIsNullOrderByIdAsc(UserRole.ADMIN)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_USER));

        return createOrGetChatRoom(currentUserId, new ChatRoomCreateRequest(adminUser.getId()));
    }

    @Transactional
    public ChatRoomResponse createGroupChatRoom(Integer currentUserId, ChatRoomCreateRequest.Group request) {
        User creator = userRepository.getById(currentUserId);

        List<Integer> distinctUserIds = request.userIds().stream()
            .distinct()
            .filter(id -> !id.equals(currentUserId))
            .toList();

        if (distinctUserIds.isEmpty()) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }

        List<User> invitees = userRepository.findAllByIdIn(distinctUserIds);
        if (invitees.size() != distinctUserIds.size()) {
            throw CustomException.of(NOT_FOUND_USER);
        }

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.groupOf());
        LocalDateTime joinedAt = Objects.requireNonNull(
            chatRoom.getCreatedAt(), "chatRoom.createdAt must not be null"
        );

        List<ChatRoomMember> members = new ArrayList<>();
        members.add(ChatRoomMember.ofOwner(chatRoom, creator, joinedAt));
        invitees.forEach(user -> members.add(ChatRoomMember.of(chatRoom, user, joinedAt)));
        chatRoomMemberRepository.saveAll(members);

        return ChatRoomResponse.from(chatRoom);
    }

    @Transactional
    public void leaveChatRoom(Integer userId, Integer roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isClubGroupRoom()) {
            throw CustomException.of(CANNOT_LEAVE_GROUP_CHAT_ROOM);
        }

        ChatRoomMember member = getRoomMember(roomId, userId);
        if (room.isDirectRoom()) {
            member.leaveDirectRoom(LocalDateTime.now());
            return;
        }

        chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, userId);
    }

    @Transactional
    public void kickMember(Integer requesterId, Integer roomId, Integer targetUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        validateGroupRoomForKick(room);
        validateNotSelfKick(requesterId, targetUserId);

        ChatRoomMember requester = getRoomMember(roomId, requesterId);
        validateKickAuthority(requester);

        ChatRoomMember target = getRoomMember(roomId, targetUserId);
        validateNotOwnerTarget(target);

        chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, targetUserId);
    }

    public ChatRoomsSummaryResponse getChatRooms(Integer userId) {
        chatRoomMembershipService.ensureClubRoomMemberships(userId);

        List<ChatRoomSummaryResponse> directRooms = getDirectChatRooms(userId);
        List<ChatRoomSummaryResponse> clubRooms = getClubChatRooms(userId);
        List<ChatRoomSummaryResponse> groupRooms = getGroupChatRooms(userId);

        List<Integer> roomIds = new ArrayList<>();
        roomIds.addAll(directRooms.stream().map(ChatRoomSummaryResponse::roomId).toList());
        roomIds.addAll(clubRooms.stream().map(ChatRoomSummaryResponse::roomId).toList());
        roomIds.addAll(groupRooms.stream().map(ChatRoomSummaryResponse::roomId).toList());

        Map<Integer, Boolean> muteMap = getMuteMap(roomIds, userId);
        Map<Integer, String> customRoomNameMap = getCustomRoomNameMap(roomIds, userId);
        List<ChatRoomSummaryResponse> rooms = new ArrayList<>();

        directRooms.forEach(room -> rooms.add(new ChatRoomSummaryResponse(
            room.roomId(),
            room.chatType(),
            resolveRoomName(room.roomId(), room.roomName(), customRoomNameMap),
            room.roomImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.createdAt(),
            room.unreadCount(),
            muteMap.getOrDefault(room.roomId(), false)
        )));

        clubRooms.forEach(room -> rooms.add(new ChatRoomSummaryResponse(
            room.roomId(),
            room.chatType(),
            resolveRoomName(room.roomId(), room.roomName(), customRoomNameMap),
            room.roomImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.createdAt(),
            room.unreadCount(),
            muteMap.getOrDefault(room.roomId(), false)
        )));

        groupRooms.forEach(room -> rooms.add(new ChatRoomSummaryResponse(
            room.roomId(),
            room.chatType(),
            resolveRoomName(room.roomId(), room.roomName(), customRoomNameMap),
            room.roomImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.createdAt(),
            room.unreadCount(),
            muteMap.getOrDefault(room.roomId(), false)
        )));

        rooms.sort(
            Comparator.comparing(
                (ChatRoomSummaryResponse room) ->
                    room.lastSentAt() != null ? room.lastSentAt() : room.createdAt(),
                Comparator.reverseOrder()
            )
        );

        return new ChatRoomsSummaryResponse(rooms);
    }

    public ChatInvitableUsersResponse getInvitableUsers(
        Integer userId,
        String query,
        ChatInviteSortBy sortBy,
        Integer page,
        Integer limit
    ) {
        userRepository.getById(userId);
        PageRequest pageRequest = PageRequest.of(page - 1, limit);

        if (sortBy == ChatInviteSortBy.CLUB) {
            return getInvitableUsersGroupedByClub(userId, query, pageRequest);
        }

        Page<User> filteredUserEntitiesPage = chatInviteQueryRepository.findInvitableUsers(userId, query, pageRequest);

        // 응답 DTO는 채팅 초대 화면에서 바로 쓰는 최소 필드만 유지한다.
        List<ChatInvitableUsersResponse.InvitableUser> filteredUsers = filteredUserEntitiesPage.getContent().stream()
            .map(ChatInvitableUsersResponse.InvitableUser::from)
            .toList();

        // 응답 메타(total/current page 정보)는 유지하면서 내용만 DTO로 치환한다.
        Page<ChatInvitableUsersResponse.InvitableUser> filteredUsersPage = new PageImpl<>(
            filteredUsers,
            pageRequest,
            filteredUserEntitiesPage.getTotalElements()
        );

        return ChatInvitableUsersResponse.forNameSort(filteredUsersPage);
    }

    private ChatInvitableUsersResponse getInvitableUsersGroupedByClub(
        Integer userId,
        String query,
        PageRequest pageRequest
    ) {
        // CLUB 정렬은 DB가 현재 페이지에 들어갈 userId까지 잘라 오고,
        // 서비스는 그 결과를 섹션 응답으로만 복원한다.
        Page<Integer> pagedUserIds = chatInviteQueryRepository.findInvitableUserIdsGroupedByClub(
            userId,
            query,
            pageRequest
        );

        if (pagedUserIds.isEmpty()) {
            return ChatInvitableUsersResponse.forClubSort(
                new PageImpl<>(List.of(), pageRequest, pagedUserIds.getTotalElements()),
                List.of()
            );
        }

        // IN 조회는 정렬 순서를 보장하지 않으므로, DB가 정한 userId 페이지 순서대로 다시 조립한다.
        Map<Integer, User> pagedUserMap = userRepository.findAllByIdIn(pagedUserIds.getContent()).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        List<ChatInvitableUsersResponse.InvitableUser> pagedUsers = pagedUserIds.getContent().stream()
            .map(pagedUserMap::get)
            .filter(Objects::nonNull)
            .map(ChatInvitableUsersResponse.InvitableUser::from)
            .toList();

        Page<ChatInvitableUsersResponse.InvitableUser> pagedInvitableUsers = new PageImpl<>(
            pagedUsers,
            pageRequest,
            pagedUserIds.getTotalElements()
        );

        record SectionKey(Integer clubId, String clubName) {
        }

        Map<Integer, Integer> representativeClubByUserId = new HashMap<>();
        Map<Integer, String> representativeClubNames = new HashMap<>();
        // 현재 페이지 사용자에 대해서만 대표 동아리를 다시 구해도,
        // userId 자체는 이미 대표 동아리 기준으로 정렬돼 있으므로 페이지 경계는 유지된다.
        chatInviteQueryRepository.findSharedClubMemberships(userId, pagedUserIds.getContent()).stream()
            .forEach(clubMember -> {
                representativeClubNames.putIfAbsent(clubMember.getClub().getId(), clubMember.getClub().getName());
                representativeClubByUserId.putIfAbsent(clubMember.getUser().getId(), clubMember.getClub().getId());
            });

        // 대표 동아리가 없는 사용자는 기타 섹션으로 떨어지고,
        // 같은 대표 동아리를 가진 사용자끼리만 현재 페이지 sections[]로 묶는다.
        Map<SectionKey, List<ChatInvitableUsersResponse.InvitableUser>> sectionMap = new LinkedHashMap<>();
        pagedUsers.forEach(user -> {
            Integer representativeClubId = representativeClubByUserId.get(user.userId());
            String clubName = representativeClubId == null
                ? ETC_SECTION_NAME
                : representativeClubNames.get(representativeClubId);
            SectionKey key = new SectionKey(representativeClubId, clubName);
            sectionMap.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(user);
        });

        List<ChatInvitableUsersResponse.InvitableSection> sections = sectionMap.entrySet().stream()
            .map(entry -> new ChatInvitableUsersResponse.InvitableSection(
                entry.getKey().clubId(),
                entry.getKey().clubName(),
                entry.getValue()
            ))
            .toList();

        return ChatInvitableUsersResponse.forClubSort(pagedInvitableUsers, sections);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatMessagePageResponse getMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        LocalDateTime readAt = LocalDateTime.now();

        if (room.isDirectRoom()) {
            chatRoomMembershipService.updateDirectRoomLastReadAt(roomId, userId, readAt);
            recordPresenceSafely(roomId, userId);
            return getDirectChatRoomMessages(userId, roomId, page, limit, readAt);
        }

        if (room.isClubGroupRoom()) {
            chatRoomMembershipService.ensureClubRoomMember(roomId, userId);
            chatRoomMembershipService.updateLastReadAt(roomId, userId, readAt);
            recordPresenceSafely(roomId, userId);
            return getClubMessagesByRoomId(roomId, userId, page, limit);
        }

        getAccessibleRoomMember(room, userId);
        chatRoomMembershipService.updateLastReadAt(roomId, userId, readAt);
        recordPresenceSafely(roomId, userId);
        return getGroupMessagesByRoomId(roomId, userId, page, limit);
    }

    @Transactional
    public ChatMessageDetailResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isDirectRoom()) {
            return sendDirectMessage(userId, roomId, request);
        }

        if (room.isClubGroupRoom()) {
            return sendClubMessageByRoomId(roomId, userId, request.content());
        }

        return sendGroupMessageByRoomId(roomId, userId, request.content());
    }

    @Transactional
    public ChatMuteResponse toggleMute(Integer userId, Integer roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CHAT_ROOM));
        User user = userRepository.getById(userId);

        if (room.isClubGroupRoom()) {
            ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
            ensureRoomMember(room, member.getUser(), member.getCreatedAt());
        } else if (room.isDirectRoom()) {
            getAccessibleDirectRoomMember(room, user);
        } else {
            getAccessibleRoomMember(room, userId);
        }
        Boolean isMuted = notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(
                NotificationTargetType.CHAT_ROOM,
                roomId,
                userId
            )
            .map(setting -> {
                setting.toggleMute();
                notificationMuteSettingRepository.save(setting);
                return setting.getIsMuted();
            })
            .orElseGet(() -> {
                notificationMuteSettingRepository.save(NotificationMuteSetting.of(
                    NotificationTargetType.CHAT_ROOM,
                    roomId,
                    user,
                    true
                ));
                return true;
            });

        return new ChatMuteResponse(isMuted);
    }

    @Transactional
    public void updateChatRoomName(Integer userId, Integer roomId, ChatRoomNameUpdateRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        ChatRoomMember roomMember = getAccessibleRoomMember(room, userId);
        roomMember.updateCustomRoomName(normalizeCustomRoomName(request.roomName()));
    }

    private List<ChatRoomSummaryResponse> getDirectChatRooms(Integer userId) {
        User user = userRepository.getById(userId);

        if (user.getRole() == UserRole.ADMIN) {
            return getAdminDirectChatRooms();
        }

        List<ChatRoomSummaryResponse> roomSummaries = new ArrayList<>();
        List<ChatRoom> personalChatRooms = chatRoomRepository.findByUserId(userId, ChatType.DIRECT);
        Map<Integer, List<ChatRoomMember>> roomMembersMap = getRoomMembersMap(personalChatRooms);
        Map<Integer, Integer> personalUnreadCountMap = getUnreadCountMap(extractChatRoomIds(personalChatRooms), userId);

        for (ChatRoom chatRoom : personalChatRooms) {
            List<ChatRoomMember> members = roomMembersMap.getOrDefault(chatRoom.getId(), List.of());
            ChatRoomMember currentMember = findRoomMember(members, userId);
            if (currentMember == null || !isDirectRoomVisibleToUser(chatRoom, currentMember)) {
                continue;
            }

            User chatPartner = resolveDirectChatPartner(members, user.getId());
            if (chatPartner == null) {
                continue;
            }

            roomSummaries.add(new ChatRoomSummaryResponse(
                chatRoom.getId(),
                ChatType.DIRECT,
                chatPartner.getName(),
                chatPartner.getImageUrl(),
                getVisibleLastMessageContent(chatRoom, currentMember),
                getVisibleLastMessageSentAt(chatRoom, currentMember),
                chatRoom.getCreatedAt(),
                personalUnreadCountMap.getOrDefault(chatRoom.getId(), 0),
                false
            ));
        }

        roomSummaries.sort(Comparator
            .comparing(
                (ChatRoomSummaryResponse room) ->
                    room.lastSentAt() != null ? room.lastSentAt() : room.createdAt(),
                Comparator.reverseOrder()
            ));

        return roomSummaries;
    }

    private List<ChatRoomSummaryResponse> getAdminDirectChatRooms() {
        List<AdminChatRoomProjection> projections = chatRoomRepository.findAdminChatRoomsOptimized(
            SYSTEM_ADMIN_ID, UserRole.ADMIN, ChatType.DIRECT
        );

        return projections.stream()
            .map(projection -> new ChatRoomSummaryResponse(
                projection.roomId(),
                ChatType.DIRECT,
                projection.nonAdminUserName(),
                projection.nonAdminImageUrl(),
                projection.lastMessage(),
                projection.lastSentAt(),
                projection.createdAt(),
                projection.unreadCount().intValue(),
                false
            ))
            .toList();
    }

    private List<ChatRoomSummaryResponse> getClubChatRooms(Integer userId) {
        List<ClubMember> memberships = clubMemberRepository.findAllByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Integer> clubIds = memberships.stream()
            .map(cm -> cm.getClub().getId())
            .toList();

        List<ChatRoom> rooms = chatRoomRepository.findByClubIds(new ArrayList<>(clubIds))
            .stream()
            .filter(room -> room.getClub() != null)
            .toList();

        List<Integer> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        Map<Integer, ChatMessage> lastMessageMap = getLastMessageMap(roomIds);
        Map<Integer, Integer> unreadCountMap = getRoomUnreadCountMap(roomIds, userId);

        return rooms.stream()
            .map(room -> {
                ChatMessage lastMessage = lastMessageMap.get(room.getId());
                return new ChatRoomSummaryResponse(
                    room.getId(),
                    ChatType.CLUB_GROUP,
                    room.getClub().getName(),
                    room.getClub().getImageUrl(),
                    lastMessage != null ? lastMessage.getContent() : null,
                    lastMessage != null ? lastMessage.getCreatedAt() : null,
                    room.getCreatedAt(),
                    unreadCountMap.getOrDefault(room.getId(), 0),
                    false
                );
            })
            .toList();
    }

    private List<ChatRoomSummaryResponse> getGroupChatRooms(Integer userId) {
        List<ChatRoom> rooms = chatRoomRepository.findGroupRoomsByMemberUserId(userId);
        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Integer> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        Map<Integer, ChatMessage> lastMessageMap = getLastMessageMap(roomIds);
        Map<Integer, Integer> unreadCountMap = getRoomUnreadCountMap(roomIds, userId);

        return rooms.stream()
            .map(room -> {
                ChatMessage lastMessage = lastMessageMap.get(room.getId());
                return new ChatRoomSummaryResponse(
                    room.getId(),
                    ChatType.GROUP,
                    DEFAULT_GROUP_ROOM_NAME,
                    null,
                    lastMessage != null ? lastMessage.getContent() : null,
                    lastMessage != null ? lastMessage.getCreatedAt() : null,
                    room.getCreatedAt(),
                    unreadCountMap.getOrDefault(room.getId(), 0),
                    false
                );
            })
            .toList();
    }

    private ChatMessagePageResponse getDirectChatRoomMessages(
        Integer userId,
        Integer roomId,
        Integer page,
        Integer limit,
        LocalDateTime readAt
    ) {
        ChatRoom chatRoom = getDirectRoom(roomId);
        User user = userRepository.getById(userId);
        ChatRoomMember member = getOrCreateDirectRoomMember(chatRoom, user);
        LocalDateTime visibleMessageFrom = prepareDirectRoomAccess(member, chatRoom);

        boolean isAdminViewingSystemRoom = user.getRole() == UserRole.ADMIN && isSystemAdminRoom(chatRoom);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, visibleMessageFrom, pageable);
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);

        List<LocalDateTime> sortedReadBaselines = isAdminViewingSystemRoom
            ? toAdminChatReadBaselines(members)
            : toSortedReadBaselines(members);

        Integer maskedAdminId = getMaskedAdminId(user, chatRoom);
        List<ChatMessageDetailResponse> responseMessages = messages.getContent().stream()
            .map(message -> {
                Integer senderId = resolveDirectSenderId(message, maskedAdminId);
                boolean isMine = shouldDisplayAsOwnMessage(user, message, isAdminViewingSystemRoom);
                boolean isRead = isMine || !message.getCreatedAt().isAfter(readAt);
                int unreadCount = countUnreadSince(message.getCreatedAt(), sortedReadBaselines);
                return new ChatMessageDetailResponse(
                    message.getId(),
                    senderId,
                    null,
                    message.getContent(),
                    message.getCreatedAt(),
                    isRead,
                    unreadCount,
                    isMine
                );
            })
            .toList();

        return new ChatMessagePageResponse(
            messages.getTotalElements(),
            messages.getNumberOfElements(),
            messages.getTotalPages(),
            messages.getNumber() + 1,
            null,
            responseMessages
        );
    }

    private ChatMessageDetailResponse sendDirectMessage(
        Integer userId,
        Integer roomId,
        ChatMessageSendRequest request
    ) {
        ChatRoom chatRoom = getDirectRoom(roomId);
        User sender = userRepository.getById(userId);
        ChatRoomMember senderMember = getAccessibleDirectRoomMember(chatRoom, sender);
        boolean senderHadLeft = senderMember.hasLeft();
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        User receiver = resolveDirectChatPartner(members, userId);

        ChatMessage chatMessage = chatMessageRepository.save(
            ChatMessage.of(chatRoom, sender, request.content())
        );
        if (senderHadLeft) {
            senderMember.restoreDirectRoom();
        }
        chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());
        updateMemberLastReadAt(roomId, userId, chatMessage.getCreatedAt());
        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        notificationService.sendChatNotification(receiver.getId(), roomId, sender.getName(), request.content());

        boolean isSystemAdminRoom = members.stream()
            .map(ChatRoomMember::getUserId)
            .anyMatch(memberUserId -> memberUserId.equals(SYSTEM_ADMIN_ID));
        publishAdminChatEventIfNeeded(isSystemAdminRoom, sender, request.content());

        return new ChatMessageDetailResponse(
            chatMessage.getId(),
            chatMessage.getSender().getId(),
            null,
            chatMessage.getContent(),
            chatMessage.getCreatedAt(),
            true,
            countUnreadSince(chatMessage.getCreatedAt(), sortedReadBaselines),
            true
        );
    }

    private ChatMessagePageResponse getClubMessagesByRoomId(
        Integer roomId,
        Integer userId,
        Integer page,
        Integer limit
    ) {
        ChatRoom room = getClubRoom(roomId);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        long totalCount = chatMessageRepository.countByChatRoomId(roomId, null);
        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomId(roomId, null, pageable);
        List<ChatMessage> messages = messagePage.getContent();
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        List<ChatMessageDetailResponse> responseMessages = messages.stream()
            .map(message -> {
                int unreadCount = countUnreadSince(message.getCreatedAt(), sortedReadBaselines);
                return new ChatMessageDetailResponse(
                    message.getId(),
                    message.getSender().getId(),
                    message.getSender().getName(),
                    message.getContent(),
                    message.getCreatedAt(),
                    null,
                    unreadCount,
                    message.isSentBy(userId)
                );
            })
            .toList();

        int totalPage = limit > 0 ? (int)Math.ceil((double)totalCount / (double)limit) : 0;
        return new ChatMessagePageResponse(
            totalCount,
            responseMessages.size(),
            totalPage,
            page,
            room.getClub().getId(),
            responseMessages
        );
    }

    private ChatMessageDetailResponse sendClubMessageByRoomId(Integer roomId, Integer userId, String content) {
        ChatRoom room = getClubRoom(roomId);
        ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
        User sender = member.getUser();

        ensureRoomMember(room, sender, member.getCreatedAt());

        ChatMessage message = chatMessageRepository.save(ChatMessage.of(room, sender, content));
        room.updateLastMessage(message.getContent(), message.getCreatedAt());
        updateLastReadAt(roomId, userId, message.getCreatedAt());

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        List<Integer> recipientUserIds = members.stream().map(ChatRoomMember::getUserId).toList();
        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        notificationService.sendGroupChatNotification(
            roomId,
            sender.getId(),
            room.getClub().getName(),
            sender.getName(),
            message.getContent(),
            recipientUserIds
        );

        return new ChatMessageDetailResponse(
            message.getId(),
            sender.getId(),
            sender.getName(),
            message.getContent(),
            message.getCreatedAt(),
            null,
            countUnreadSince(message.getCreatedAt(), sortedReadBaselines),
            true
        );
    }

    private ChatMessagePageResponse getGroupMessagesByRoomId(
        Integer roomId,
        Integer userId,
        Integer page,
        Integer limit
    ) {
        chatRoomRepository.getById(roomId);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        long totalCount = chatMessageRepository.countByChatRoomId(roomId, null);
        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomId(roomId, null, pageable);
        List<ChatMessage> messages = messagePage.getContent();
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        List<ChatMessageDetailResponse> responseMessages = messages.stream()
            .map(message -> {
                int unreadCount = countUnreadSince(message.getCreatedAt(), sortedReadBaselines);
                return new ChatMessageDetailResponse(
                    message.getId(),
                    message.getSender().getId(),
                    message.getSender().getName(),
                    message.getContent(),
                    message.getCreatedAt(),
                    null,
                    unreadCount,
                    message.isSentBy(userId)
                );
            })
            .toList();

        int totalPage = limit > 0 ? (int)Math.ceil((double)totalCount / (double)limit) : 0;
        return new ChatMessagePageResponse(
            totalCount,
            responseMessages.size(),
            totalPage,
            page,
            null,
            responseMessages
        );
    }

    private ChatMessageDetailResponse sendGroupMessageByRoomId(Integer roomId, Integer userId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
        User sender = userRepository.getById(userId);

        ChatRoomMember senderMember = getRoomMember(roomId, userId);
        if (senderMember.hasLeft()) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.of(room, sender, content));
        room.updateLastMessage(message.getContent(), message.getCreatedAt());
        updateLastReadAt(roomId, userId, message.getCreatedAt());

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        List<Integer> recipientUserIds = members.stream()
            .map(ChatRoomMember::getUserId)
            .filter(id -> !id.equals(userId))
            .toList();
        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        notificationService.sendGroupChatNotification(
            roomId,
            sender.getId(),
            DEFAULT_GROUP_ROOM_NAME,
            sender.getName(),
            message.getContent(),
            recipientUserIds
        );

        return new ChatMessageDetailResponse(
            message.getId(),
            sender.getId(),
            sender.getName(),
            message.getContent(),
            message.getCreatedAt(),
            null,
            countUnreadSince(message.getCreatedAt(), sortedReadBaselines),
            true
        );
    }

    private Map<Integer, Boolean> getMuteMap(List<Integer> roomIds, Integer userId) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        List<NotificationMuteSetting> settings = notificationMuteSettingRepository
            .findByTargetTypeAndTargetIdsAndUserId(NotificationTargetType.CHAT_ROOM, roomIds, userId);

        Map<Integer, Boolean> muteMap = new HashMap<>();
        for (NotificationMuteSetting setting : settings) {
            Integer targetId = setting.getTargetId();
            if (targetId != null) {
                muteMap.put(targetId, setting.getIsMuted());
            }
        }

        return muteMap;
    }

    private Map<Integer, String> getCustomRoomNameMap(List<Integer> roomIds, Integer userId) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        return chatRoomMemberRepository.findByChatRoomIdsAndUserId(roomIds, userId).stream()
            .filter(member -> StringUtils.hasText(member.getCustomRoomName()))
            .collect(Collectors.toMap(ChatRoomMember::getChatRoomId, ChatRoomMember::getCustomRoomName));
    }

    private String resolveRoomName(Integer roomId, String defaultRoomName, Map<Integer, String> customRoomNameMap) {
        return customRoomNameMap.getOrDefault(roomId, defaultRoomName);
    }

    private ChatRoom getDirectRoom(Integer roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (!chatRoom.isDirectRoom()) {
            throw CustomException.of(NOT_FOUND_CHAT_ROOM);
        }

        return chatRoom;
    }

    private ChatRoom getClubRoom(Integer roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CHAT_ROOM));
        if (!room.isClubGroupRoom()) {
            throw CustomException.of(ApiResponseCode.NOT_FOUND_GROUP_CHAT_ROOM);
        }
        return room;
    }

    private List<Integer> extractChatRoomIds(List<ChatRoom> chatRooms) {
        return chatRooms.stream()
            .map(ChatRoom::getId)
            .toList();
    }

    private Map<Integer, Integer> getUnreadCountMap(List<Integer> chatRoomIds, Integer userId) {
        if (chatRoomIds.isEmpty()) {
            return Map.of();
        }

        List<UnreadMessageCount> unreadMessageCounts = chatMessageRepository.countUnreadMessagesByChatRoomIdsAndUserId(
            chatRoomIds,
            userId
        );

        return unreadMessageCounts.stream()
            .collect(Collectors.toMap(
                UnreadMessageCount::chatRoomId,
                unreadMessageCount -> unreadMessageCount.unreadCount().intValue()
            ));
    }

    private Integer getMaskedAdminId(User user, ChatRoom chatRoom) {
        if (user.getRole() == UserRole.ADMIN) {
            return null;
        }

        List<Object[]> memberResults = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(
            List.of(chatRoom.getId())
        );
        List<MemberInfo> memberInfos = memberResults.stream()
            .map(row -> new MemberInfo((Integer)row[1], (LocalDateTime)row[2]))
            .toList();

        boolean hasSystemAdmin = memberInfos.stream()
            .anyMatch(info -> info.userId().equals(SYSTEM_ADMIN_ID));

        if (hasSystemAdmin) {
            return SYSTEM_ADMIN_ID;
        }

        return null;
    }

    private void publishAdminChatEventIfNeeded(boolean isSystemAdminRoom, User sender, String content) {
        if (isSystemAdminRoom && sender.getRole() != UserRole.ADMIN) {
            eventPublisher.publishEvent(AdminChatReceivedEvent.of(sender.getId(), sender.getName(), content));
        }
    }

    private ChatRoomMember getRoomMember(Integer roomId, Integer userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
            .orElseThrow(() -> CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS));
    }

    private ChatRoomMember getAccessibleRoomMember(ChatRoom room, Integer userId) {
        if (room.isClubGroupRoom()) {
            ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
            ensureRoomMember(room, member.getUser(), member.getCreatedAt());
            return getRoomMember(room.getId(), userId);
        }

        if (room.isDirectRoom()) {
            User user = userRepository.getById(userId);
            return getAccessibleDirectRoomMember(room, user);
        }

        return getRoomMember(room.getId(), userId);
    }

    private void ensureRoomMember(ChatRoom room, User user, LocalDateTime joinedAt) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                LocalDateTime lastReadAt = member.getLastReadAt();
                if (lastReadAt == null || lastReadAt.isBefore(joinedAt)) {
                    member.updateLastReadAt(joinedAt);
                }
            }, () -> chatRoomMemberRepository.save(ChatRoomMember.of(room, user, joinedAt)));
    }

    private void ensureDirectRoomRequester(ChatRoom room, User user, LocalDateTime joinedAt) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                if (member.hasLeft()) {
                    member.reopenDirectRoom(LocalDateTime.now());
                    return;
                }

                LocalDateTime lastReadAt = member.getLastReadAt();
                if (lastReadAt == null || lastReadAt.isBefore(joinedAt)) {
                    member.updateLastReadAt(joinedAt);
                }
            }, () -> chatRoomMemberRepository.save(ChatRoomMember.of(room, user, joinedAt)));
    }

    private String normalizeCustomRoomName(String roomName) {
        if (!StringUtils.hasText(roomName)) {
            return null;
        }

        return roomName.trim();
    }

    private void updateMemberLastReadAt(Integer roomId, Integer userId, LocalDateTime lastReadAt) {
        int updated = chatRoomMemberRepository.updateLastReadAtIfOlder(roomId, userId, lastReadAt);
        if (updated == 0) {
            ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
            User user = userRepository.getById(userId);
            ensureRoomMember(room, user, lastReadAt);
        }
    }

    private void updateLastReadAt(Integer roomId, Integer userId, LocalDateTime lastReadAt) {
        int updated = chatRoomMemberRepository.updateLastReadAtIfOlder(roomId, userId, lastReadAt);
        if (updated == 0) {
            ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
            User user = userRepository.getById(userId);
            ensureRoomMember(room, user, lastReadAt);
        }
    }

    private List<LocalDateTime> toSortedReadBaselines(List<ChatRoomMember> members) {
        List<LocalDateTime> baselines = members.stream()
            .map(ChatRoomMember::getLastReadAt)
            .sorted()
            .toList();
        return baselines;
    }

    private List<LocalDateTime> toAdminChatReadBaselines(List<ChatRoomMember> members) {
        LocalDateTime adminLastReadAt = null;
        LocalDateTime userLastReadAt = null;

        for (ChatRoomMember member : members) {
            if (member.getUser().getRole() == UserRole.ADMIN) {
                if (adminLastReadAt == null || member.getLastReadAt().isAfter(adminLastReadAt)) {
                    adminLastReadAt = member.getLastReadAt();
                }
            } else {
                userLastReadAt = member.getLastReadAt();
            }
        }

        List<LocalDateTime> baselines = new ArrayList<>();
        if (adminLastReadAt != null) {
            baselines.add(adminLastReadAt);
        }
        if (userLastReadAt != null) {
            baselines.add(userLastReadAt);
        }
        baselines.sort(Comparator.naturalOrder());
        return baselines;
    }

    private int countUnreadSince(LocalDateTime messageCreatedAt, List<LocalDateTime> sortedReadBaselines) {
        int left = 0;
        int right = sortedReadBaselines.size();

        while (left < right) {
            int mid = (left + right) >>> 1;
            LocalDateTime baseline = sortedReadBaselines.get(mid);

            if (baseline.isBefore(messageCreatedAt)) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

    private Map<Integer, ChatMessage> getLastMessageMap(List<Integer> roomIds) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        return chatMessageRepository.findLatestMessagesByRoomIds(roomIds).stream()
            .collect(Collectors.toMap(message -> message.getChatRoom().getId(), message -> message));
    }

    private Map<Integer, Integer> getRoomUnreadCountMap(List<Integer> roomIds, Integer userId) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Integer> unreadCountMap = new HashMap<>();
        List<RoomUnreadCountProjection> projections = chatRoomMemberRepository.countUnreadByRoomIdsAndUserId(
            roomIds,
            userId
        );
        for (RoomUnreadCountProjection projection : projections) {
            unreadCountMap.put(projection.getRoomId(), projection.getUnreadCount().intValue());
        }

        Set<Integer> existingRoomIds = unreadCountMap.keySet();
        for (Integer roomId : roomIds) {
            if (!existingRoomIds.contains(roomId)) {
                unreadCountMap.put(roomId, 0);
            }
        }

        return unreadCountMap;
    }

    private ChatRoomMember getOrCreateDirectRoomMember(ChatRoom chatRoom, User user) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
            .orElseGet(() -> {
                if (user.getRole() == UserRole.ADMIN && isSystemAdminRoom(chatRoom)) {
                    LocalDateTime joinedAt = LocalDateTime.now();
                    return chatRoomMemberRepository.save(ChatRoomMember.of(chatRoom, user, joinedAt));
                }
                throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
            });
    }

    private ChatRoomMember getAccessibleDirectRoomMember(ChatRoom chatRoom, User user) {
        ChatRoomMember member = getOrCreateDirectRoomMember(chatRoom, user);
        restoreDirectRoomIfVisible(member, chatRoom);
        return member;
    }

    private LocalDateTime prepareDirectRoomAccess(ChatRoomMember member, ChatRoom chatRoom) {
        LocalDateTime visibleMessageFrom = member.getVisibleMessageFrom();
        restoreDirectRoomIfVisible(member, chatRoom);
        return visibleMessageFrom;
    }

    /**
     * direct 채팅방에서 나간 사용자가 다시 볼 수 있는 상태인지 확인하고,
     * 새 메시지가 이미 존재하면 나간 상태를 해제한다.
     */
    private void restoreDirectRoomIfVisible(ChatRoomMember member, ChatRoom chatRoom) {
        if (!member.hasLeft()) {
            return;
        }

        if (!member.hasVisibleMessages(chatRoom)) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        member.restoreDirectRoom();
    }

    private boolean isSystemAdminRoom(ChatRoom chatRoom) {
        List<Object[]> memberIds = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(
            List.of(chatRoom.getId())
        );
        List<Integer> userIds = memberIds.stream()
            .map(row -> (Integer)row[1])
            .toList();

        return userIds.contains(SYSTEM_ADMIN_ID);
    }

    private boolean shouldDisplayAsOwnMessage(
        User currentUser,
        ChatMessage message,
        boolean isAdminViewingSystemRoom
    ) {
        if (isAdminViewingSystemRoom) {
            return message.getSender().getRole() == UserRole.ADMIN;
        }
        return message.isSentBy(currentUser.getId());
    }

    private Integer resolveDirectSenderId(ChatMessage message, Integer maskedAdminId) {
        if (maskedAdminId != null && message.getSender().getRole() == UserRole.ADMIN) {
            return maskedAdminId;
        }
        return message.getSender().getId();
    }

    private ChatRoomMember findRoomMember(List<ChatRoomMember> members, Integer userId) {
        return members.stream()
            .filter(member -> member.getUserId().equals(userId))
            .findFirst()
            .orElse(null);
    }

    private boolean isDirectRoomVisibleToUser(ChatRoom room, ChatRoomMember member) {
        return !member.hasLeft() || member.hasVisibleMessages(room);
    }

    private String getVisibleLastMessageContent(ChatRoom room, ChatRoomMember member) {
        if (!member.hasVisibleMessages(room)) {
            return null;
        }
        return room.getLastMessageContent();
    }

    private LocalDateTime getVisibleLastMessageSentAt(ChatRoom room, ChatRoomMember member) {
        if (!member.hasVisibleMessages(room)) {
            return null;
        }
        return room.getLastMessageSentAt();
    }

    private Map<Integer, List<ChatRoomMember>> getRoomMembersMap(List<ChatRoom> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }

        List<Integer> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        return chatRoomMemberRepository.findByChatRoomIds(roomIds).stream()
            .collect(Collectors.groupingBy(ChatRoomMember::getChatRoomId));
    }

    private Map<Integer, List<MemberInfo>> getRoomMemberInfoMap(List<ChatRoom> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }

        List<Integer> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        List<Object[]> results = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(roomIds);

        Map<Integer, List<MemberInfo>> roomMemberInfoMap = new HashMap<>();
        for (Object[] row : results) {
            Integer chatRoomId = (Integer)row[0];
            Integer memberId = (Integer)row[1];
            LocalDateTime createdAt = (LocalDateTime)row[2];
            roomMemberInfoMap.computeIfAbsent(chatRoomId, k -> new ArrayList<>())
                .add(new MemberInfo(memberId, createdAt));
        }
        return roomMemberInfoMap;
    }

    private User findDirectPartner(List<ChatRoomMember> members, Integer userId) {
        return members.stream()
            .map(ChatRoomMember::getUser)
            .filter(memberUser -> !memberUser.getId().equals(userId))
            .findFirst()
            .orElse(null);
    }

    private User resolveDirectChatPartner(List<ChatRoomMember> members, Integer userId) {
        boolean hasSystemAdmin = members.stream()
            .map(ChatRoomMember::getUserId)
            .anyMatch(memberUserId -> memberUserId.equals(SYSTEM_ADMIN_ID));

        if (hasSystemAdmin) {
            return members.stream()
                .map(ChatRoomMember::getUser)
                .filter(memberUser -> memberUser.getId().equals(SYSTEM_ADMIN_ID))
                .findFirst()
                .orElse(null);
        }

        return findDirectPartner(members, userId);
    }

    private User findDirectPartnerFromMemberInfo(
        List<MemberInfo> memberInfos,
        Integer userId,
        Map<Integer, User> userMap
    ) {
        return memberInfos.stream()
            .filter(info -> !info.userId().equals(userId))
            .min(Comparator.comparing(MemberInfo::createdAt))
            .map(info -> userMap.get(info.userId()))
            .orElse(null);
    }

    private User resolveDirectChatPartner(
        List<MemberInfo> memberInfos,
        Integer userId,
        Map<Integer, User> userMap
    ) {
        boolean hasSystemAdmin = memberInfos.stream()
            .anyMatch(info -> info.userId().equals(SYSTEM_ADMIN_ID));

        if (hasSystemAdmin) {
            return userMap.get(SYSTEM_ADMIN_ID);
        }

        return findDirectPartnerFromMemberInfo(memberInfos, userId, userMap);
    }

    private User findNonAdminUserFromMemberInfo(List<MemberInfo> memberInfos, Map<Integer, User> userMap) {
        return memberInfos.stream()
            .sorted(Comparator.comparing(MemberInfo::createdAt))
            .map(info -> userMap.get(info.userId()))
            .filter(Objects::nonNull)
            .filter(user -> user.getRole() != UserRole.ADMIN)
            .findFirst()
            .orElse(null);
    }

    private User resolveMessageReceiverFromMemberInfo(
        User sender,
        List<MemberInfo> memberInfos,
        Map<Integer, User> userMap
    ) {
        if (sender.getRole() == UserRole.ADMIN) {
            User nonAdminUser = findNonAdminUserFromMemberInfo(memberInfos, userMap);
            if (nonAdminUser != null) {
                return nonAdminUser;
            }
        }

        User partner = findDirectPartnerFromMemberInfo(memberInfos, sender.getId(), userMap);
        if (partner == null) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }
        return partner;
    }

    private void validateGroupRoomForKick(ChatRoom room) {
        if (!room.isGroupRoom() || room.isClubGroupRoom()) {
            throw CustomException.of(CANNOT_KICK_IN_NON_GROUP_ROOM);
        }
    }

    private void validateNotSelfKick(Integer requesterId, Integer targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw CustomException.of(CANNOT_KICK_SELF);
        }
    }

    private void validateKickAuthority(ChatRoomMember requester) {
        if (!requester.isOwner()) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_KICK);
        }
    }

    private void validateNotOwnerTarget(ChatRoomMember target) {
        if (target.isOwner()) {
            throw CustomException.of(CANNOT_KICK_ROOM_OWNER);
        }
    }

    private void recordPresenceSafely(Integer roomId, Integer userId) {
        try {
            chatPresenceService.recordPresence(roomId, userId);
        } catch (Exception e) {
            log.warn("Redis presence record failed, continuing: roomId={}, userId={}", roomId, userId, e);
        }
    }

    private record MemberInfo(Integer userId, LocalDateTime createdAt) {
    }
}
