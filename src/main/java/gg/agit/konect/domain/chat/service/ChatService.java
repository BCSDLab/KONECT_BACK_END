package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_USER;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMuteResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomSummaryResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsSummaryResponse;
import gg.agit.konect.domain.chat.dto.UnreadMessageCount;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.event.AdminChatReceivedEvent;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.repository.RoomUnreadCountProjection;
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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int SYSTEM_ADMIN_ID = 1;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Integer currentUserId, ChatRoomCreateRequest request) {
        User currentUser = userRepository.getById(currentUserId);
        User targetUser = userRepository.getById(request.userId());

        if (currentUser.getId().equals(targetUser.getId())) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }
        ChatRoom.validateIsNotSameParticipant(currentUser, targetUser);

        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(currentUser.getId(), targetUser.getId())
            .orElseGet(() -> chatRoomRepository.save(ChatRoom.directOf()));

        LocalDateTime joinedAt = Objects.requireNonNull(chatRoom.getCreatedAt(), "chatRoom.createdAt must not be null");
        ensureRoomMember(chatRoom, currentUser, joinedAt);
        ensureRoomMember(chatRoom, targetUser, joinedAt);

        return ChatRoomResponse.from(chatRoom);
    }

    @Transactional
    public ChatRoomResponse createOrGetAdminChatRoom(Integer currentUserId) {
        User adminUser = userRepository.findFirstByRoleOrderByIdAsc(UserRole.ADMIN)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_USER));

        return createOrGetChatRoom(currentUserId, new ChatRoomCreateRequest(adminUser.getId()));
    }

    @Transactional
    public ChatRoomsSummaryResponse getChatRooms(Integer userId) {
        List<ChatRoomSummaryResponse> directRooms = getDirectChatRooms(userId);
        List<ChatRoomSummaryResponse> clubRooms = getClubChatRooms(userId);

        List<Integer> roomIds = new ArrayList<>();
        roomIds.addAll(directRooms.stream().map(ChatRoomSummaryResponse::roomId).toList());
        roomIds.addAll(clubRooms.stream().map(ChatRoomSummaryResponse::roomId).toList());

        Map<Integer, Boolean> muteMap = getMuteMap(roomIds, userId);
        List<ChatRoomSummaryResponse> rooms = new ArrayList<>();

        directRooms.forEach(room -> rooms.add(new ChatRoomSummaryResponse(
            room.roomId(),
            room.chatType(),
            room.roomName(),
            room.roomImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.unreadCount(),
            muteMap.getOrDefault(room.roomId(), false)
        )));

        clubRooms.forEach(room -> rooms.add(new ChatRoomSummaryResponse(
            room.roomId(),
            room.chatType(),
            room.roomName(),
            room.roomImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.unreadCount(),
            muteMap.getOrDefault(room.roomId(), false)
        )));

        rooms.sort(
            Comparator.comparing(ChatRoomSummaryResponse::lastSentAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ChatRoomSummaryResponse::roomId)
        );

        return new ChatRoomsSummaryResponse(rooms);
    }

    @Transactional
    public ChatMessagePageResponse getMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isDirectRoom()) {
            return getDirectChatRoomMessages(userId, roomId, page, limit);
        }

        return getClubMessagesByRoomId(roomId, userId, page, limit);
    }

    @Transactional
    public ChatMessageDetailResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isDirectRoom()) {
            return sendDirectMessage(userId, roomId, request);
        }

        return sendClubMessageByRoomId(roomId, userId, request.content());
    }

    @Transactional
    public ChatMuteResponse toggleMute(Integer userId, Integer roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CHAT_ROOM));
        User user = userRepository.getById(userId);

        if (room.isGroupRoom()) {
            ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
            ensureRoomMember(room, member.getUser(), member.getCreatedAt());
        } else {
            getOrCreateDirectRoomMember(room, user);
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

    private List<ChatRoomSummaryResponse> getDirectChatRooms(Integer userId) {
        User user = userRepository.getById(userId);

        if (user.getRole() == UserRole.ADMIN) {
            return getAdminDirectChatRooms();
        }

        List<ChatRoomSummaryResponse> roomSummaries = new ArrayList<>();
        List<ChatRoom> personalChatRooms = chatRoomRepository.findByUserId(userId);
        Map<Integer, List<MemberInfo>> roomMemberInfoMap = getRoomMemberInfoMap(personalChatRooms);
        Map<Integer, Integer> personalUnreadCountMap = getUnreadCountMap(extractChatRoomIds(personalChatRooms), userId);

        List<Integer> allUserIds = roomMemberInfoMap.values().stream()
            .flatMap(List::stream)
            .map(MemberInfo::userId)
            .distinct()
            .toList();

        Map<Integer, User> userMap = allUserIds.isEmpty()
            ? Map.of()
            : userRepository.findAllByIdIn(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        for (ChatRoom chatRoom : personalChatRooms) {
            List<MemberInfo> memberInfos = roomMemberInfoMap.getOrDefault(chatRoom.getId(), List.of());
            User chatPartner = resolveDirectChatPartner(memberInfos, user.getId(), userMap);
            if (chatPartner == null) {
                continue;
            }

            roomSummaries.add(new ChatRoomSummaryResponse(
                chatRoom.getId(),
                ChatType.DIRECT,
                chatPartner.getName(),
                chatPartner.getImageUrl(),
                chatRoom.getLastMessageContent(),
                chatRoom.getLastMessageSentAt(),
                personalUnreadCountMap.getOrDefault(chatRoom.getId(), 0),
                false
            ));
        }

        roomSummaries.sort(Comparator
            .comparing(
                ChatRoomSummaryResponse::lastSentAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            )
            .thenComparing(ChatRoomSummaryResponse::roomId));

        return roomSummaries;
    }

    private List<ChatRoomSummaryResponse> getAdminDirectChatRooms() {
        List<ChatRoomSummaryResponse> roomSummaries = new ArrayList<>();

        List<ChatRoom> adminUserRooms = chatRoomRepository.findAllSystemAdminDirectRooms(
            SYSTEM_ADMIN_ID, UserRole.ADMIN
        );
        Map<Integer, List<MemberInfo>> roomMemberInfoMap = getRoomMemberInfoMap(adminUserRooms);
        Map<Integer, Integer> adminUnreadCountMap = getAdminUnreadCountMap(extractChatRoomIds(adminUserRooms));

        List<Integer> allUserIds = roomMemberInfoMap.values().stream()
            .flatMap(List::stream)
            .map(MemberInfo::userId)
            .distinct()
            .toList();

        Map<Integer, User> userMap = allUserIds.isEmpty()
            ? Map.of()
            : userRepository.findAllByIdIn(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        for (ChatRoom chatRoom : adminUserRooms) {
            List<MemberInfo> memberInfos = roomMemberInfoMap.getOrDefault(chatRoom.getId(), List.of());
            User nonAdminUser = findNonAdminUserFromMemberInfo(memberInfos, userMap);
            if (nonAdminUser == null) {
                continue;
            }
            if (!chatMessageRepository.existsUserReplyByRoomId(chatRoom.getId(), UserRole.ADMIN)) {
                continue;
            }

            roomSummaries.add(new ChatRoomSummaryResponse(
                chatRoom.getId(),
                ChatType.DIRECT,
                nonAdminUser.getName(),
                nonAdminUser.getImageUrl(),
                chatRoom.getLastMessageContent(),
                chatRoom.getLastMessageSentAt(),
                adminUnreadCountMap.getOrDefault(chatRoom.getId(), 0),
                false
            ));
        }

        roomSummaries.sort(Comparator
            .comparing(
                ChatRoomSummaryResponse::lastSentAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            )
            .thenComparing(ChatRoomSummaryResponse::roomId));

        return roomSummaries;
    }

    private ChatMessagePageResponse getDirectChatRoomMessages(
        Integer userId,
        Integer roomId,
        Integer page,
        Integer limit
    ) {
        ChatRoom chatRoom = getDirectRoom(roomId);
        User user = userRepository.getById(userId);
        ChatRoomMember member = getOrCreateDirectRoomMember(chatRoom, user);

        LocalDateTime readAt = LocalDateTime.now();
        chatPresenceService.recordPresence(roomId, userId);
        member.updateLastReadAt(readAt);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, pageable);

        Integer maskedAdminId = getMaskedAdminId(user, chatRoom);
        List<ChatMessageDetailResponse> responseMessages = messages.getContent().stream()
            .map(message -> {
                Integer senderId = resolveDirectSenderId(message, maskedAdminId);
                boolean isMine = message.isSentBy(userId);
                boolean isRead = isMine || !message.getCreatedAt().isAfter(readAt);
                return new ChatMessageDetailResponse(
                    message.getId(),
                    senderId,
                    null,
                    message.getContent(),
                    message.getCreatedAt(),
                    isRead,
                    null,
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
        getOrCreateDirectRoomMember(chatRoom, sender);

        List<Object[]> memberResults = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(List.of(roomId));
        List<MemberInfo> memberInfos = memberResults.stream()
            .map(row -> new MemberInfo((Integer)row[1], (LocalDateTime)row[2]))
            .toList();

        List<Integer> memberUserIds = memberInfos.stream().map(MemberInfo::userId).toList();
        Map<Integer, User> userMap = userRepository.findAllByIdIn(memberUserIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        User receiver = resolveMessageReceiverFromMemberInfo(sender, memberInfos, userMap);

        ChatMessage chatMessage = chatMessageRepository.save(
            ChatMessage.of(chatRoom, sender, request.content())
        );
        chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());
        updateMemberLastReadAt(roomId, userId, chatMessage.getCreatedAt());

        notificationService.sendChatNotification(receiver.getId(), roomId, sender.getName(), request.content());

        boolean isSystemAdminRoom = memberInfos.stream()
            .anyMatch(info -> info.userId().equals(SYSTEM_ADMIN_ID));
        publishAdminChatEventIfNeeded(isSystemAdminRoom, sender, request.content());

        return new ChatMessageDetailResponse(
            chatMessage.getId(),
            chatMessage.getSender().getId(),
            null,
            chatMessage.getContent(),
            chatMessage.getCreatedAt(),
            true,
            null,
            true
        );
    }

    private List<ChatRoomSummaryResponse> getClubChatRooms(Integer userId) {
        List<ClubMember> memberships = clubMemberRepository.findAllByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<Integer, ClubMember> membershipByClubId = memberships.stream()
            .collect(Collectors.toMap(cm -> cm.getClub().getId(), cm -> cm, (a, b) -> a));

        List<ChatRoom> rooms = memberships.stream()
            .map(ClubMember::getClub)
            .map(this::resolveOrCreateClubRoom)
            .toList();

        for (ChatRoom room : rooms) {
            ClubMember member = membershipByClubId.get(room.getClub().getId());
            if (member != null) {
                ensureRoomMember(room, member.getUser(), member.getCreatedAt());
            }
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
                    room.getClub().getName(),
                    room.getClub().getImageUrl(),
                    lastMessage != null ? lastMessage.getContent() : null,
                    lastMessage != null ? lastMessage.getCreatedAt() : null,
                    unreadCountMap.getOrDefault(room.getId(), 0),
                    false
                );
            })
            .toList();
    }

    private ChatMessagePageResponse getClubMessagesByRoomId(
        Integer roomId,
        Integer userId,
        Integer page,
        Integer limit
    ) {
        ChatRoom room = getClubRoom(roomId);
        ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
        ensureRoomMember(room, member.getUser(), member.getCreatedAt());

        chatPresenceService.recordPresence(roomId, userId);
        updateLastReadAt(roomId, userId, LocalDateTime.now());

        PageRequest pageable = PageRequest.of(page - 1, limit);
        long totalCount = chatMessageRepository.countByChatRoomId(roomId);
        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomId(roomId, pageable);
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
        if (!room.isGroupRoom() || room.getClub() == null) {
            throw CustomException.of(ApiResponseCode.NOT_FOUND_GROUP_CHAT_ROOM);
        }
        return room;
    }

    private ChatRoom resolveOrCreateClubRoom(Club club) {
        return chatRoomRepository.findByClubId(club.getId())
            .orElseGet(() -> chatRoomRepository.save(ChatRoom.groupOf(club)));
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

    private Map<Integer, Integer> getAdminUnreadCountMap(List<Integer> chatRoomIds) {
        if (chatRoomIds.isEmpty()) {
            return Map.of();
        }

        List<UnreadMessageCount> unreadMessageCounts = chatMessageRepository.countUnreadMessagesForAdmin(
            chatRoomIds,
            UserRole.ADMIN
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

    private void ensureRoomMember(ChatRoom room, User user, LocalDateTime joinedAt) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                LocalDateTime lastReadAt = member.getLastReadAt();
                if (lastReadAt == null || lastReadAt.isBefore(joinedAt)) {
                    member.updateLastReadAt(joinedAt);
                }
            }, () -> chatRoomMemberRepository.save(ChatRoomMember.of(room, user, joinedAt)));
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
            ChatRoom room = getClubRoom(roomId);
            ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
            ensureRoomMember(room, member.getUser(), member.getCreatedAt());
        }
    }

    private List<LocalDateTime> toSortedReadBaselines(List<ChatRoomMember> members) {
        List<LocalDateTime> baselines = members.stream()
            .map(ChatRoomMember::getLastReadAt)
            .sorted()
            .toList();
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

    private boolean isSystemAdminRoom(ChatRoom chatRoom) {
        List<Object[]> memberIds = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(
            List.of(chatRoom.getId())
        );
        List<Integer> userIds = memberIds.stream()
            .map(row -> (Integer)row[1])
            .toList();

        return userIds.contains(SYSTEM_ADMIN_ID);
    }

    private Integer resolveDirectSenderId(ChatMessage message, Integer maskedAdminId) {
        if (maskedAdminId != null && message.getSender().getRole() == UserRole.ADMIN) {
            return maskedAdminId;
        }
        return message.getSender().getId();
    }

    private Map<Integer, List<ChatRoomMember>> getRoomMembersMap(List<ChatRoom> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }

        List<Integer> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        return chatRoomMemberRepository.findByChatRoomIds(roomIds).stream()
            .collect(Collectors.groupingBy(ChatRoomMember::getChatRoomId));
    }

    private record MemberInfo(Integer userId, LocalDateTime createdAt) {
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

    private User findNonAdminMember(List<ChatRoomMember> members) {
        return members.stream()
            .map(ChatRoomMember::getUser)
            .filter(memberUser -> memberUser.getRole() != UserRole.ADMIN)
            .findFirst()
            .orElse(null);
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

    private User resolveMessageReceiver(User sender, List<ChatRoomMember> members) {
        if (sender.getRole() == UserRole.ADMIN) {
            User nonAdminUser = findNonAdminMember(members);
            if (nonAdminUser != null) {
                return nonAdminUser;
            }
        }

        User partner = findDirectPartner(members, sender.getId());
        if (partner == null) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }
        return partner;
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

}
