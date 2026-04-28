package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ChatRoomMemberCommandService chatRoomMemberCommandService;
    private final ChatRoomSummaryService chatRoomSummaryService;
    private final ChatSearchService chatSearchService;
    private final ChatInviteService chatInviteService;
    private final ChatMessageReadService chatMessageReadService;
    private final ChatMessagePageResolver chatMessagePageResolver;
    private final ChatRoomAccessService chatRoomAccessService;
    private final ChatRoomCreationService chatRoomCreationService;
    private final ChatRoomSystemAdminService chatRoomSystemAdminService;
    private final ChatDirectRoomAccessService chatDirectRoomAccessService;
    private final ChatMessageSendService chatMessageSendService;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Integer currentUserId, ChatRoomCreateRequest request) {
        return chatRoomCreationService.createOrGetChatRoom(currentUserId, request);
    }

    @Transactional
    public ChatRoomResponse createOrGetAdminChatRoom(Integer currentUserId) {
        return chatRoomCreationService.createOrGetAdminChatRoom(currentUserId);
    }

    @Transactional
    public ChatRoomResponse createGroupChatRoom(Integer currentUserId, ChatRoomCreateRequest.Group request) {
        return chatRoomCreationService.createGroupChatRoom(currentUserId, request);
    }

    @Transactional
    public void leaveChatRoom(Integer userId, Integer roomId) {
        chatRoomMemberCommandService.leaveChatRoom(userId, roomId);
    }

    @Transactional
    public void kickMember(Integer requesterId, Integer roomId, Integer targetUserId) {
        chatRoomMemberCommandService.kickMember(requesterId, roomId, targetUserId);
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

        chatRoomAccessService.getAccessibleMember(room, userId);
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

        chatRoomAccessService.ensureMuteAccess(room, user);
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

        ChatRoomMember roomMember = chatRoomAccessService.getAccessibleMember(room, userId);
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

    private ChatRoomMember getRoomMember(Integer roomId, Integer userId) {
        return ChatRoomMemberLookup.getRoomMember(chatRoomMemberRepository, roomId, userId);
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

    private String normalizeCustomRoomName(String roomName) {
        if (!StringUtils.hasText(roomName)) {
            return null;
        }

        return roomName.trim();
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
