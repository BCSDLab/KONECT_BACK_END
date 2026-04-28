package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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
import gg.agit.konect.domain.chat.dto.ChatSearchResponse;
import gg.agit.konect.domain.chat.dto.UnreadMessageCount;
import gg.agit.konect.domain.chat.enums.ChatInviteSortBy;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomQueryRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.repository.RoomUnreadCountProjection;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
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

    private static final String DEFAULT_GROUP_ROOM_NAME = "그룹 채팅";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomQueryRepository chatRoomQueryRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final ChatRoomMembershipService chatRoomMembershipService;
    private final ChatRoomSummaryService chatRoomSummaryService;
    private final ChatSearchService chatSearchService;
    private final ChatInviteService chatInviteService;
    private final ChatMessageReadService chatMessageReadService;
    private final ChatMessagePageResolver chatMessagePageResolver;
    private final ChatRoomSystemAdminService chatRoomSystemAdminService;
    private final ChatDirectRoomAccessService chatDirectRoomAccessService;
    private final ChatMessageSendService chatMessageSendService;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Integer currentUserId, ChatRoomCreateRequest request) {
        User currentUser = userRepository.getById(currentUserId);
        User targetUser = userRepository.getById(request.userId());

        if (currentUser.getId().equals(targetUser.getId())) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }

        if (currentUser.isAdmin() && !targetUser.isAdmin()) {
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
        List<ChatRoomSummaryResponse> directRooms = getDirectChatRooms(userId);
        List<ChatRoomSummaryResponse> clubRooms = getClubChatRooms(userId);
        List<ChatRoomSummaryResponse> groupRooms = getGroupChatRooms(userId);

        List<ChatRoomSummaryResponse> rooms = chatRoomSummaryService.summarizeChatRooms(
                userId,
                directRooms,
                clubRooms,
                groupRooms
        );

        return new ChatRoomsSummaryResponse(rooms);
    }

    public ChatSearchResponse searchChats(Integer userId, String keyword, Integer page, Integer limit) {
        AccessibleChatRooms accessibleChatRooms = getAccessibleChatRooms(userId);
        return chatSearchService.search(userId, keyword, accessibleChatRooms.rooms(),
                accessibleChatRooms.defaultRoomNameMap(), page, limit);
    }

    public ChatInvitableUsersResponse getInvitableUsers(
            Integer userId,
            String query,
            ChatInviteSortBy sortBy,
            Integer page,
            Integer limit
    ) {
        return chatInviteService.getInvitableUsers(userId, query, sortBy, page, limit);
    }

    @Transactional
    public ChatMessagePageResponse getMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        return getMessages(userId, roomId, page, limit, null);
    }

    @Transactional
    public ChatMessagePageResponse getMessages(
            Integer userId, Integer roomId, Integer page, Integer limit, Integer messageId
    ) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
        User user = userRepository.getById(userId);

        if (messageId != null) {
            page = chatMessagePageResolver.resolvePageForMessage(roomId, messageId, room, user, limit);
        }

        LocalDateTime readAt = LocalDateTime.now();

        if (room.isDirectRoom()) {
            boolean isAdminViewingSystemRoom = user.isAdmin()
                    && chatRoomSystemAdminService.isSystemAdminRoom(room.getId());
            if (isAdminViewingSystemRoom) {
                chatRoomMembershipService.updateLastReadAt(roomId, SYSTEM_ADMIN_ID, readAt);
                recordPresenceSafely(roomId, userId);
                return chatMessageReadService.getAdminSystemDirectChatRoomMessages(user, room, page, limit, readAt);
            }

            chatRoomMembershipService.updateDirectRoomLastReadAt(roomId, user, readAt, room);
            recordPresenceSafely(roomId, userId);
            return chatMessageReadService.getDirectChatRoomMessages(user, room, page, limit, readAt);
        }

        if (room.isClubGroupRoom()) {
            chatRoomMembershipService.ensureClubRoomMember(roomId, userId);
            chatRoomMembershipService.updateLastReadAt(roomId, userId, readAt);
            recordPresenceSafely(roomId, userId);
            return chatMessageReadService.getClubMessagesByRoom(room, userId, page, limit);
        }

        getAccessibleRoomMember(room, userId);
        chatRoomMembershipService.updateLastReadAt(roomId, userId, readAt);
        recordPresenceSafely(roomId, userId);
        return chatMessageReadService.getGroupMessagesByRoom(roomId, userId, page, limit);
    }

    @Transactional
    public ChatMessageDetailResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        return chatMessageSendService.sendMessage(userId, roomId, request);
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
            // 어드민이 SYSTEM_ADMIN 방에 접근하는 경우는 멤버십 체크를 건너뜀
            boolean isAdminAccessingSystemAdminRoom = user.isAdmin()
                    && chatRoomSystemAdminService.isSystemAdminRoom(room.getId());
            if (!isAdminAccessingSystemAdminRoom) {
                chatDirectRoomAccessService.getAccessibleMember(room, user);
            }
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

        if (user.isAdmin()) {
            return getAdminDirectChatRooms(userId);
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

    private List<ChatRoomSummaryResponse> getAdminDirectChatRooms(Integer adminUserId) {
        List<AdminChatRoomProjection> projections = chatRoomQueryRepository.findAdminChatRoomsOptimized(
                SYSTEM_ADMIN_ID, adminUserId, UserRole.ADMIN, ChatType.DIRECT
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
        Map<Integer, Integer> unreadCountMap = getRoomUnreadCountMap(roomIds, userId);

        return rooms.stream()
                .map(room -> new ChatRoomSummaryResponse(
                        room.getId(),
                        ChatType.CLUB_GROUP,
                        room.getClub().getName(),
                        room.getClub().getImageUrl(),
                        room.getLastMessageContent(),
                        room.getLastMessageSentAt(),
                        room.getCreatedAt(),
                        unreadCountMap.getOrDefault(room.getId(), 0),
                        false
                ))
                .toList();
    }

    private List<ChatRoomSummaryResponse> getGroupChatRooms(Integer userId) {
        List<ChatRoom> rooms = chatRoomRepository.findGroupRoomsByMemberUserId(userId);
        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Integer> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        Map<Integer, Integer> unreadCountMap = getRoomUnreadCountMap(roomIds, userId);

        return rooms.stream()
                .map(room -> new ChatRoomSummaryResponse(
                        room.getId(),
                        ChatType.GROUP,
                        DEFAULT_GROUP_ROOM_NAME,
                        null,
                        room.getLastMessageContent(),
                        room.getLastMessageSentAt(),
                        room.getCreatedAt(),
                        unreadCountMap.getOrDefault(room.getId(), 0),
                        false
                ))
                .toList();
    }

    private ChatMessagePageResponse buildDirectChatRoomMessages(
            User user,
            Integer roomId,
            Integer page,
            Integer limit,
            LocalDateTime readAt,
            LocalDateTime visibleMessageFrom,
            List<LocalDateTime> sortedReadBaselines,
            Integer maskedAdminId
    ) {
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, visibleMessageFrom, pageable);

        List<ChatMessageDetailResponse> responseMessages = messages.getContent().stream()
                .map(message -> {
                    Integer senderId = maskedAdminId != null
                            ? resolveDirectSenderId(message, maskedAdminId)
                            : message.getSender().getId();
                    boolean isMine = maskedAdminId != null
                            ? shouldDisplayAsOwnMessage(user, message, true)
                            : message.isSentBy(user.getId());
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

    private ChatMessagePageResponse getDirectChatRoomMessages(
            Integer userId,
            Integer roomId,
            Integer page,
            Integer limit,
            LocalDateTime readAt
    ) {
        ChatRoom chatRoom = getDirectRoom(roomId);
        User user = userRepository.getById(userId);
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        LocalDateTime visibleMessageFrom =
                chatDirectRoomAccessService.prepareAccessAndGetVisibleMessageFrom(chatRoom, user);

        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        return buildDirectChatRoomMessages(user, roomId, page, limit, readAt,
                visibleMessageFrom, sortedReadBaselines, null);
    }

    private ChatMessagePageResponse getAdminSystemDirectChatRoomMessages(
            User user,
            ChatRoom chatRoom,
            Integer roomId,
            Integer page,
            Integer limit,
            LocalDateTime readAt
    ) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        LocalDateTime visibleMessageFrom = resolveAdminSystemRoomVisibleMessageFrom(members);

        List<LocalDateTime> sortedReadBaselines = toAdminChatReadBaselines(members);
        Integer maskedAdminId = getMaskedAdminId(user, chatRoom);

        return buildDirectChatRoomMessages(user, roomId, page, limit, readAt,
                visibleMessageFrom, sortedReadBaselines, maskedAdminId);
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

        int totalPage = limit > 0 ? (int) Math.ceil((double) totalCount / (double) limit) : 0;
        return new ChatMessagePageResponse(
                totalCount,
                responseMessages.size(),
                totalPage,
                page,
                room.getClub().getId(),
                responseMessages
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

        int totalPage = limit > 0 ? (int) Math.ceil((double) totalCount / (double) limit) : 0;
        return new ChatMessagePageResponse(
                totalCount,
                responseMessages.size(),
                totalPage,
                page,
                null,
                responseMessages
        );
    }

    private AccessibleChatRooms getAccessibleChatRooms(Integer userId) {
        List<ChatRoomSummaryResponse> directRooms = getDirectChatRooms(userId);
        List<ChatRoomSummaryResponse> clubRooms = getClubChatRooms(userId);

        Map<Integer, String> defaultRoomNameMap = chatRoomSummaryService.getDefaultRoomNameMap(
                directRooms,
                clubRooms
        );
        List<ChatRoomSummaryResponse> rooms = chatRoomSummaryService.summarizeSearchableRooms(
                userId,
                directRooms,
                clubRooms
        );
        return new AccessibleChatRooms(rooms, defaultRoomNameMap);
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
        if (user.isAdmin()) {
            return null;
        }

        List<Object[]> memberResults = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(
                List.of(chatRoom.getId())
        );
        List<MemberInfo> memberInfos = memberResults.stream()
                .map(row -> new MemberInfo((Integer) row[1], (LocalDateTime) row[2]))
                .toList();

        boolean hasSystemAdmin = memberInfos.stream()
                .anyMatch(info -> info.userId().equals(SYSTEM_ADMIN_ID));

        if (hasSystemAdmin) {
            return SYSTEM_ADMIN_ID;
        }

        return null;
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
            return chatDirectRoomAccessService.getAccessibleMember(room, user);
        }

        ChatRoomMember member = getRoomMember(room.getId(), userId);

        if (member.hasLeft()) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        return member;
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
        if (shouldSkipSystemAdminMembership(room, user)) {
            return;
        }

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

    private boolean shouldSkipSystemAdminMembership(ChatRoom room, User user) {
        // 문의방은 SYSTEM_ADMIN + 일반 사용자 2인 구조를 전제로 재사용(findByTwoUsers)되므로,
        // 생성/재오픈 경로에서도 일반 ADMIN을 멤버로 추가하면 안 된다.
        return user.isAdmin() && chatRoomSystemAdminService.isSystemAdminRoom(room.getId());
    }

    private String normalizeCustomRoomName(String roomName) {
        if (!StringUtils.hasText(roomName)) {
            return null;
        }

        return roomName.trim();
    }

    private List<LocalDateTime> toSortedReadBaselines(List<ChatRoomMember> members) {
        return members.stream()
                .map(ChatRoomMember::getLastReadAt)
                .sorted()
                .toList();
    }

    private List<LocalDateTime> toAdminChatReadBaselines(List<ChatRoomMember> members) {
        LocalDateTime adminLastReadAt = null;
        LocalDateTime userLastReadAt = null;

        for (ChatRoomMember member : members) {
            if (member.getUser().isAdmin()) {
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

    private LocalDateTime resolveAdminSystemRoomVisibleMessageFrom(List<ChatRoomMember> members) {
        ChatRoomMember systemAdminMember = chatRoomSystemAdminService.findSystemAdminMember(members);
        return systemAdminMember != null ? systemAdminMember.getVisibleMessageFrom() : null;
    }

    private boolean shouldDisplayAsOwnMessage(
            User currentUser,
            ChatMessage message,
            boolean isAdminViewingSystemRoom
    ) {
        if (isAdminViewingSystemRoom) {
            return message.getSender().isAdmin();
        }
        return message.isSentBy(currentUser.getId());
    }

    private Integer resolveDirectSenderId(ChatMessage message, Integer maskedAdminId) {
        if (maskedAdminId != null && message.getSender().isAdmin()) {
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
            Integer chatRoomId = (Integer) row[0];
            Integer memberId = (Integer) row[1];
            LocalDateTime createdAt = (LocalDateTime) row[2];
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

    private record AccessibleChatRooms(
            List<ChatRoomSummaryResponse> rooms,
            Map<Integer, String> defaultRoomNameMap
    ) {
    }

    private record MemberInfo(Integer userId, LocalDateTime createdAt) {
    }
}
