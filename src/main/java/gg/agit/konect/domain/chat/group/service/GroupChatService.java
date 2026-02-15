package gg.agit.konect.domain.chat.group.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.direct.model.ChatRoom;
import gg.agit.konect.domain.chat.direct.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.group.dto.GroupChatMessageResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatMessagesResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatRoomResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatRoomsResponse;
import gg.agit.konect.domain.chat.group.model.GroupChatMessage;
import gg.agit.konect.domain.chat.group.model.GroupChatReadStatus;
import gg.agit.konect.domain.chat.group.model.GroupChatRoom;
import gg.agit.konect.domain.chat.group.repository.GroupChatMessageRepository;
import gg.agit.konect.domain.chat.group.repository.GroupChatReadStatusRepository;
import gg.agit.konect.domain.chat.group.repository.GroupChatRoomRepository;
import gg.agit.konect.domain.chat.group.repository.GroupRoomUnreadCountProjection;
import gg.agit.konect.domain.chat.unified.enums.ChatType;
import gg.agit.konect.domain.chat.unified.service.ChatPresenceService;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupChatService {

    private final GroupChatRoomRepository groupChatRoomRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final GroupChatReadStatusRepository groupChatReadStatusRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;

    public GroupChatRoomResponse getGroupChatRoom(Integer clubId, Integer userId) {
        validateClubMember(clubId, userId);

        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);
        return new GroupChatRoomResponse(room.getId());
    }

    public GroupChatRoomsResponse getChatRooms(Integer userId) {
        List<GroupChatRoom> rooms = groupChatRoomRepository.findAllByUserId(userId);
        List<Integer> roomIds = rooms.stream()
            .map(GroupChatRoom::getId)
            .toList();

        Map<Integer, GroupChatMessage> lastMessageMap = getLastMessageMap(roomIds);
        Map<Integer, Integer> unreadCountMap = getUnreadCountMap(roomIds, userId);

        return GroupChatRoomsResponse.from(rooms, lastMessageMap, unreadCountMap);
    }

    @Transactional
    public GroupChatMessagesResponse getMessagesByRoomId(Integer roomId, Integer userId, Integer page, Integer limit) {
        GroupChatRoom room = groupChatRoomRepository.getById(roomId);
        return getMessages(room.getClub().getId(), userId, page, limit);
    }

    @Transactional
    public GroupChatMessageResponse sendMessageByRoomId(Integer roomId, Integer userId, String content) {
        GroupChatRoom room = groupChatRoomRepository.getById(roomId);
        return sendMessage(room.getClub().getId(), userId, content);
    }

    @Transactional
    public GroupChatMessagesResponse getMessages(Integer clubId, Integer userId, Integer page, Integer limit) {
        validateClubMember(clubId, userId);

        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);
        LocalDateTime joinedAt = clubMemberRepository.getJoinedAtByClubIdAndUserId(clubId, userId);
        Integer roomId = room.getId();

        chatPresenceService.recordPresence(roomId, userId);

        updateLastReadAt(roomId, userId, LocalDateTime.now());

        PageRequest pageable = PageRequest.of(page - 1, limit);
        long totalCount = groupChatMessageRepository.countByRoomIdAndCreatedAtGreaterThanEqual(roomId, joinedAt);
        Page<GroupChatMessage> messagePage = groupChatMessageRepository
            .findByRoomIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(roomId, joinedAt, pageable);
        List<GroupChatMessage> messages = messagePage.getContent();

        List<ClubMember> members = clubMemberRepository.findAllByClubId(clubId);
        Map<Integer, LocalDateTime> lastReadAtMap = groupChatReadStatusRepository.findByRoomId(roomId).stream()
            .collect(java.util.stream.Collectors.toMap(
                GroupChatReadStatus::getUserId,
                GroupChatReadStatus::getLastReadAt
            ));

        List<GroupChatMessageResponse> responseMessages = messages.stream()
            .map(message -> {
                Integer messageId = message.getId();
                Integer senderId = message.getSender().getId();
                String senderName = message.getSender().getName();
                String messageContent = message.getContent();
                LocalDateTime createdAt = message.getCreatedAt();

                int unreadCount = getUnreadCount(message, members, lastReadAtMap);
                boolean isMine = senderId.equals(userId);

                return new GroupChatMessageResponse(
                    messageId,
                    senderId,
                    senderName,
                    messageContent,
                    createdAt,
                    unreadCount,
                    isMine
                );
            })
            .toList();

        int totalPage = limit > 0 ? (int)Math.ceil((double)totalCount / (double)limit) : 0;

        return new GroupChatMessagesResponse(
            totalCount,
            responseMessages.size(),
            totalPage,
            page,
            clubId,
            responseMessages
        );
    }

    @Transactional
    public GroupChatMessageResponse sendMessage(Integer clubId, Integer userId, String content) {
        validateClubMember(clubId, userId);

        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);
        Integer roomId = room.getId();
        User sender = userRepository.getById(userId);

        GroupChatMessage message = groupChatMessageRepository.save(GroupChatMessage.of(room, sender, content));
        updateLastReadAt(roomId, userId, message.getCreatedAt());

        Integer messageId = message.getId();
        Integer senderId = sender.getId();
        String senderName = sender.getName();
        String messageContent = message.getContent();
        LocalDateTime createdAt = message.getCreatedAt();

        List<Integer> recipientUserIds = clubMemberRepository.findUserIdsByClubId(clubId);
        Set<Integer> mutedUserIds = getMutedUserIds(roomId);
        List<Integer> filteredRecipients = recipientUserIds.stream()
            .filter(recipientId -> !mutedUserIds.contains(recipientId))
            .toList();

        notificationService.sendGroupChatNotification(
            roomId,
            senderId,
            senderName,
            messageContent,
            filteredRecipients
        );

        int memberCount = recipientUserIds.size();
        int unreadCount = memberCount - 1;
        return new GroupChatMessageResponse(
            messageId,
            senderId,
            senderName,
            messageContent,
            createdAt,
            unreadCount,
            true
        );
    }

    @Transactional
    public Boolean toggleMute(Integer userId, ChatType type, Integer chatRoomId) {
        NotificationTargetType targetType;

        if (type == ChatType.DIRECT) {
            ChatRoom directRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CHAT_ROOM));
            directRoom.validateIsParticipant(userId);
            targetType = NotificationTargetType.DIRECT_CHAT_ROOM;
        } else {
            GroupChatRoom groupRoom = groupChatRoomRepository.getById(chatRoomId);
            validateClubMember(groupRoom.getClub().getId(), userId);
            targetType = NotificationTargetType.GROUP_CHAT_ROOM;
        }

        User user = userRepository.getById(userId);

        return notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(
                targetType,
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
                    targetType,
                    chatRoomId,
                    user,
                    true
                ));
                return true;
            });
    }

    private void validateClubMember(Integer clubId, Integer userId) {
        if (!clubMemberRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_GROUP_CHAT_ACCESS);
        }
    }

    private void updateLastReadAt(Integer roomId, Integer userId, LocalDateTime lastReadAt) {
        groupChatReadStatusRepository.findByRoomIdAndUserId(roomId, userId)
            .ifPresentOrElse(status -> {
                status.updateLastReadAt(lastReadAt);
            }, () -> {
                GroupChatRoom room = groupChatRoomRepository.getById(roomId);
                User user = userRepository.getById(userId);
                groupChatReadStatusRepository.save(GroupChatReadStatus.of(room, user, lastReadAt));
            });
    }

    private Set<Integer> getMutedUserIds(Integer roomId) {
        List<Integer> mutedUserIds = notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndIsMutedTrue(
                NotificationTargetType.GROUP_CHAT_ROOM,
                roomId
            )
            .stream()
            .map(setting -> setting.getUser().getId())
            .toList();

        return new HashSet<>(mutedUserIds);
    }

    private int getUnreadCount(
        GroupChatMessage message,
        List<ClubMember> members,
        Map<Integer, LocalDateTime> lastReadAtMap
    ) {
        LocalDateTime messageCreatedAt = message.getCreatedAt();
        int unreadCount = 0;

        for (ClubMember member : members) {
            if (member.getCreatedAt().isAfter(messageCreatedAt)) {
                continue;
            }

            LocalDateTime lastReadAt = lastReadAtMap.getOrDefault(member.getUser().getId(), member.getCreatedAt());
            if (lastReadAt.isBefore(messageCreatedAt)) {
                unreadCount++;
            }
        }

        return unreadCount;
    }

    private Map<Integer, GroupChatMessage> getLastMessageMap(List<Integer> roomIds) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        return groupChatMessageRepository.findLatestMessagesByRoomIds(roomIds).stream()
            .collect(Collectors.toMap(
                message -> message.getRoom().getId(),
                message -> message
            ));
    }

    private Map<Integer, Integer> getUnreadCountMap(List<Integer> roomIds, Integer userId) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        return groupChatMessageRepository.countUnreadByRoomIdsAndUserId(roomIds, userId).stream()
            .collect(Collectors.toMap(
                GroupRoomUnreadCountProjection::getRoomId,
                unreadCount -> unreadCount.getUnreadCount().intValue()
            ));
    }
}
