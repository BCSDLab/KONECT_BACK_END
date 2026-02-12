package gg.agit.konect.domain.groupchat.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.groupchat.dto.GroupChatMessageResponse;
import gg.agit.konect.domain.groupchat.dto.GroupChatMessagesResponse;
import gg.agit.konect.domain.groupchat.dto.GroupChatRoomResponse;
import gg.agit.konect.domain.groupchat.model.GroupChatMessage;
import gg.agit.konect.domain.groupchat.model.GroupChatNotificationSetting;
import gg.agit.konect.domain.groupchat.model.GroupChatRoom;
import gg.agit.konect.domain.groupchat.model.MessageReadStatus;
import gg.agit.konect.domain.groupchat.repository.GroupChatMessageRepository;
import gg.agit.konect.domain.groupchat.repository.GroupChatNotificationSettingRepository;
import gg.agit.konect.domain.groupchat.repository.GroupChatRoomRepository;
import gg.agit.konect.domain.groupchat.repository.MessageReadStatusRepository;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupChatService {

    private final GroupChatRoomRepository groupChatRoomRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
    private final GroupChatNotificationSettingRepository groupChatNotificationSettingRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    public GroupChatRoomResponse getGroupChatRoom(Integer clubId, Integer userId) {
        validateClubMember(clubId, userId);

        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);
        return GroupChatRoomResponse.from(room);
    }

    @Transactional
    public GroupChatMessagesResponse getMessages(Integer clubId, Integer userId, Integer page, Integer limit) {
        ClubMember clubMember = getClubMemberOrThrow(clubId, userId);
        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);

        chatPresenceService.recordPresence(room.getId(), userId);

        User user = userRepository.getById(userId);
        LocalDateTime joinedAt = clubMember.getCreatedAt();
        markAsReadInternal(room.getId(), user, joinedAt);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        long offset = pageable.getOffset();

        Long totalCount = entityManager.createQuery(
                "SELECT COUNT(m) "
                    + "FROM GroupChatMessage m "
                    + "WHERE m.room.id = :roomId "
                    + "AND m.createdAt >= :joinedAt",
                Long.class
            )
            .setParameter("roomId", room.getId())
            .setParameter("joinedAt", joinedAt)
            .getSingleResult();

        List<GroupChatMessage> messages = entityManager.createQuery(
                "SELECT m "
                    + "FROM GroupChatMessage m "
                    + "JOIN FETCH m.sender "
                    + "WHERE m.room.id = :roomId "
                    + "AND m.createdAt >= :joinedAt "
                    + "ORDER BY m.createdAt DESC",
                GroupChatMessage.class
            )
            .setParameter("roomId", room.getId())
            .setParameter("joinedAt", joinedAt)
            .setFirstResult((int)offset)
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        int memberCount = clubMemberRepository.findAllByClubId(clubId).size();

        List<Integer> messageIds = messages.stream()
            .map(GroupChatMessage::getId)
            .toList();
        Map<Integer, Integer> readCountMap = getReadCountMap(messageIds);

        List<GroupChatMessageResponse> responseMessages = messages.stream()
            .map(message -> {
                int readCount = readCountMap.getOrDefault(message.getId(), 0);
                int unreadCount = memberCount - readCount;
                return GroupChatMessageResponse.from(message, userId, unreadCount);
            })
            .toList();

        int totalPage = limit > 0 ? (int)Math.ceil((double)totalCount / (double)limit) : 0;

        return new GroupChatMessagesResponse(
            totalCount,
            responseMessages.size(),
            totalPage,
            page,
            responseMessages
        );
    }

    @Transactional
    public GroupChatMessageResponse sendMessage(Integer clubId, Integer userId, String content) {
        validateClubMember(clubId, userId);

        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);
        User sender = userRepository.getById(userId);

        GroupChatMessage message = groupChatMessageRepository.save(GroupChatMessage.of(room, sender, content));
        messageReadStatusRepository.save(MessageReadStatus.of(message, sender));

        List<Integer> recipientUserIds = clubMemberRepository.findAllByClubId(clubId).stream()
            .map(clubMember -> clubMember.getUser().getId())
            .toList();
        Set<Integer> mutedUserIds = getMutedUserIds(room.getId());
        List<Integer> filteredRecipients = recipientUserIds.stream()
            .filter(recipientId -> !mutedUserIds.contains(recipientId))
            .toList();

        notificationService.sendGroupChatNotification(
            room.getId(),
            sender.getId(),
            sender.getName(),
            content,
            filteredRecipients
        );

        int memberCount = recipientUserIds.size();
        int unreadCount = memberCount - 1;
        return GroupChatMessageResponse.from(message, userId, unreadCount);
    }

    @Transactional
    public void markAsRead(Integer clubId, Integer userId) {
        ClubMember clubMember = getClubMemberOrThrow(clubId, userId);
        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);

        User user = userRepository.getById(userId);
        markAsReadInternal(room.getId(), user, clubMember.getCreatedAt());
    }

    @Transactional
    public Boolean toggleMute(Integer clubId, Integer userId) {
        validateClubMember(clubId, userId);

        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);
        User user = userRepository.getById(userId);

        GroupChatNotificationSetting setting = groupChatNotificationSettingRepository.findByRoomIdAndUserId(
                room.getId(),
                userId
            )
            .orElseGet(() -> groupChatNotificationSettingRepository.save(
                GroupChatNotificationSetting.of(room, user, false)
            ));

        setting.toggleMute();
        groupChatNotificationSettingRepository.save(setting);

        return setting.getIsMuted();
    }

    @Transactional
    public GroupChatRoom createGroupChatRoom(Club club) {
        return groupChatRoomRepository.findByClubId(club.getId())
            .orElseGet(() -> groupChatRoomRepository.save(GroupChatRoom.of(club)));
    }

    private ClubMember getClubMemberOrThrow(Integer clubId, Integer userId) {
        return clubMemberRepository.findByClubIdAndUserId(clubId, userId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.FORBIDDEN_GROUP_CHAT_ACCESS));
    }

    private void validateClubMember(Integer clubId, Integer userId) {
        if (!clubMemberRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_GROUP_CHAT_ACCESS);
        }
    }

    private void markAsReadInternal(Integer roomId, User user, LocalDateTime joinedAt) {
        List<GroupChatMessage> unreadMessages = entityManager.createQuery(
                "SELECT m "
                    + "FROM GroupChatMessage m "
                    + "WHERE m.room.id = :roomId "
                    + "AND m.createdAt >= :joinedAt "
                    + "AND NOT EXISTS ("
                    + "  SELECT 1 "
                    + "  FROM MessageReadStatus rs "
                    + "  WHERE rs.messageId = m.id "
                    + "  AND rs.userId = :userId"
                    + ")",
                GroupChatMessage.class
            )
            .setParameter("roomId", roomId)
            .setParameter("joinedAt", joinedAt)
            .setParameter("userId", user.getId())
            .getResultList();

        if (unreadMessages.isEmpty()) {
            return;
        }

        List<MessageReadStatus> statuses = unreadMessages.stream()
            .map(message -> MessageReadStatus.of(message, user))
            .toList();

        messageReadStatusRepository.saveAll(statuses);
    }

    private Map<Integer, Integer> getReadCountMap(List<Integer> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        List<MessageReadStatus> statuses = messageReadStatusRepository.findByMessageIdIn(messageIds);
        Map<Integer, Integer> readCountMap = new HashMap<>();
        for (MessageReadStatus status : statuses) {
            readCountMap.merge(status.getMessageId(), 1, Integer::sum);
        }
        return readCountMap;
    }

    private Set<Integer> getMutedUserIds(Integer roomId) {
        List<GroupChatNotificationSetting> mutedSettings = groupChatNotificationSettingRepository
            .findByRoomIdAndIsMutedTrue(roomId);

        Set<Integer> mutedUserIds = new HashSet<>();
        for (GroupChatNotificationSetting setting : mutedSettings) {
            mutedUserIds.add(setting.getUser().getId());
        }
        return mutedUserIds;
    }

}
