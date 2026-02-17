package gg.agit.konect.domain.chat.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ClubMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ClubMessageResponse;
import gg.agit.konect.domain.chat.dto.ClubRoomListResponse;
import gg.agit.konect.domain.chat.dto.ClubRoomResponse;
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
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;

    @Transactional
    public ClubRoomResponse getClubRoom(Integer clubId, Integer userId) {
        ClubMember member = clubMemberRepository.getByClubIdAndUserId(clubId, userId);
        ChatRoom room = resolveOrCreateClubRoom(member.getClub());
        ensureRoomMember(room, member.getUser(), member.getCreatedAt());
        return new ClubRoomResponse(room.getId());
    }

    @Transactional
    public ClubRoomListResponse getChatRooms(Integer userId) {
        List<ClubMember> memberships = clubMemberRepository.findAllByUserId(userId);
        if (memberships.isEmpty()) {
            return ClubRoomListResponse.from(List.of(), Map.of(), Map.of());
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
        Map<Integer, Integer> unreadCountMap = getUnreadCountMap(roomIds, userId);

        return ClubRoomListResponse.from(rooms, lastMessageMap, unreadCountMap);
    }

    @Transactional
    public ClubMessagePageResponse getMessagesByRoomId(Integer roomId, Integer userId, Integer page, Integer limit) {
        ChatRoom room = getClubRoom(roomId);
        ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
        ensureRoomMember(room, member.getUser(), member.getCreatedAt());
        syncClubMembers(room);

        chatPresenceService.recordPresence(roomId, userId);
        updateLastReadAt(roomId, userId, LocalDateTime.now());

        PageRequest pageable = PageRequest.of(page - 1, limit);
        long totalCount = chatMessageRepository.countByChatRoomId(roomId);
        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomId(roomId, pageable);
        List<ChatMessage> messages = messagePage.getContent();
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);

        List<ClubMessageResponse> responseMessages = messages.stream()
            .map(message -> {
                int unreadCount = getUnreadCount(message, members);
                return new ClubMessageResponse(
                    message.getId(),
                    message.getSender().getId(),
                    message.getSender().getName(),
                    message.getContent(),
                    message.getCreatedAt(),
                    unreadCount,
                    message.isSentBy(userId)
                );
            })
            .toList();

        int totalPage = limit > 0 ? (int)Math.ceil((double)totalCount / (double)limit) : 0;
        return new ClubMessagePageResponse(
            totalCount,
            responseMessages.size(),
            totalPage,
            page,
            room.getClub().getId(),
            responseMessages
        );
    }

    @Transactional
    public ClubMessageResponse sendMessageByRoomId(Integer roomId, Integer userId, String content) {
        ChatRoom room = getClubRoom(roomId);
        ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
        User sender = member.getUser();

        ensureRoomMember(room, sender, member.getCreatedAt());
        syncClubMembers(room);

        ChatMessage message = chatMessageRepository.save(ChatMessage.of(room, sender, content));
        room.updateLastMessage(message.getContent(), message.getCreatedAt());
        updateLastReadAt(roomId, userId, message.getCreatedAt());

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        List<Integer> recipientUserIds = members.stream().map(ChatRoomMember::getUserId).toList();

        notificationService.sendGroupChatNotification(
            roomId,
            sender.getId(),
            room.getClub().getName(),
            sender.getName(),
            message.getContent(),
            recipientUserIds
        );

        return new ClubMessageResponse(
            message.getId(),
            sender.getId(),
            sender.getName(),
            message.getContent(),
            message.getCreatedAt(),
            getUnreadCount(message, members),
            true
        );
    }

    @Transactional
    public Boolean toggleMute(Integer userId, Integer chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CHAT_ROOM));

        if (room.isGroupRoom()) {
            ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
            ensureRoomMember(room, member.getUser(), member.getCreatedAt());
        } else {
            validateDirectAccess(room, userId);
        }

        User user = userRepository.getById(userId);
        return notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(
                NotificationTargetType.CHAT_ROOM,
                chatRoomId,
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
                    chatRoomId,
                    user,
                    true
                ));
                return true;
            });
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

    private void syncClubMembers(ChatRoom room) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByClubId(room.getClub().getId());
        for (ClubMember clubMember : clubMembers) {
            ensureRoomMember(room, clubMember.getUser(), clubMember.getCreatedAt());
        }
    }

    private void ensureRoomMember(ChatRoom room, User user, LocalDateTime joinedAt) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                if (member.getLastReadAt().isBefore(joinedAt)) {
                    member.updateLastReadAt(joinedAt);
                }
            }, () -> chatRoomMemberRepository.save(ChatRoomMember.of(room, user, joinedAt)));
    }

    private void updateLastReadAt(Integer roomId, Integer userId, LocalDateTime lastReadAt) {
        int updated = chatRoomMemberRepository.updateLastReadAtIfOlder(roomId, userId, lastReadAt);
        if (updated == 0) {
            ChatRoom room = getClubRoom(roomId);
            ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
            ensureRoomMember(room, member.getUser(), member.getCreatedAt());
        }
    }

    private void validateDirectAccess(ChatRoom room, Integer userId) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(room.getId(), userId)) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS);
        }
    }

    private int getUnreadCount(ChatMessage message, List<ChatRoomMember> members) {
        LocalDateTime createdAt = message.getCreatedAt();
        int unreadCount = 0;

        for (ChatRoomMember member : members) {
            LocalDateTime baseline = member.getLastReadAt();
            if (baseline == null || baseline.isBefore(member.getCreatedAt())) {
                baseline = member.getCreatedAt();
            }
            if (baseline.isBefore(createdAt)) {
                unreadCount += 1;
            }
        }

        return unreadCount;
    }

    private Map<Integer, ChatMessage> getLastMessageMap(List<Integer> roomIds) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        return chatMessageRepository.findLatestMessagesByRoomIds(roomIds).stream()
            .collect(Collectors.toMap(message -> message.getChatRoom().getId(), message -> message));
    }

    private Map<Integer, Integer> getUnreadCountMap(List<Integer> roomIds, Integer userId) {
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
}
