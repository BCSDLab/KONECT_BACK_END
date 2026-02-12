package gg.agit.konect.domain.groupchat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.club.model.Club;
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

@Service
@Transactional(readOnly = true)
public class GroupChatService {

    private static final int MESSAGE_ID_INDEX = 0;
    private static final int SENDER_ID_INDEX = 1;
    private static final int SENDER_NAME_INDEX = 2;
    private static final int MESSAGE_CONTENT_INDEX = 3;
    private static final int MESSAGE_CREATED_AT_INDEX = 4;

    private final GroupChatRoomRepository groupChatRoomRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
    private final GroupChatNotificationSettingRepository groupChatNotificationSettingRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    public GroupChatService(
        GroupChatRoomRepository groupChatRoomRepository,
        GroupChatMessageRepository groupChatMessageRepository,
        MessageReadStatusRepository messageReadStatusRepository,
        GroupChatNotificationSettingRepository groupChatNotificationSettingRepository,
        ClubMemberRepository clubMemberRepository,
        UserRepository userRepository,
        ChatPresenceService chatPresenceService,
        NotificationService notificationService,
        EntityManager entityManager
    ) {
        this.groupChatRoomRepository = groupChatRoomRepository;
        this.groupChatMessageRepository = groupChatMessageRepository;
        this.messageReadStatusRepository = messageReadStatusRepository;
        this.groupChatNotificationSettingRepository = groupChatNotificationSettingRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
        this.chatPresenceService = chatPresenceService;
        this.notificationService = notificationService;
        this.entityManager = entityManager;
    }

    public GroupChatRoomResponse getGroupChatRoom(Integer clubId, Integer userId) {
        validateClubMember(clubId, userId);

        Integer roomId = getRoomIdOrThrow(clubId);
        return new GroupChatRoomResponse(roomId);
    }

    @Transactional
    public GroupChatMessagesResponse getMessages(Integer clubId, Integer userId, Integer page, Integer limit) {
        LocalDateTime joinedAt = getJoinedAtOrThrow(clubId, userId);
        Integer roomId = getRoomIdOrThrow(clubId);

        chatPresenceService.recordPresence(roomId, userId);

        markAsReadInternal(roomId, userId, joinedAt);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        long offset = pageable.getOffset();

        Long totalCount = entityManager.createQuery(
                "SELECT COUNT(m) "
                    + "FROM GroupChatMessage m "
                    + "WHERE m.room.id = :roomId "
                    + "AND m.createdAt >= :joinedAt",
                Long.class
            )
            .setParameter("roomId", roomId)
            .setParameter("joinedAt", joinedAt)
            .getSingleResult();

        List<Object[]> messageRows = entityManager.createQuery(
                "SELECT m.id, m.sender.id, m.sender.name, m.content, m.createdAt "
                    + "FROM GroupChatMessage m "
                    + "WHERE m.room.id = :roomId "
                    + "AND m.createdAt >= :joinedAt "
                    + "ORDER BY m.createdAt DESC",
                Object[].class
            )
            .setParameter("roomId", roomId)
            .setParameter("joinedAt", joinedAt)
            .setFirstResult((int)offset)
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        int memberCount = getMemberCount(clubId);

        List<Integer> messageIds = messageRows.stream()
            .map(row -> (Integer)row[MESSAGE_ID_INDEX])
            .toList();
        Map<Integer, Integer> readCountMap = getReadCountMap(messageIds);

        List<GroupChatMessageResponse> responseMessages = messageRows.stream()
            .map(row -> {
                Integer messageId = (Integer)row[MESSAGE_ID_INDEX];
                Integer senderId = (Integer)row[SENDER_ID_INDEX];
                String senderName = (String)row[SENDER_NAME_INDEX];
                String messageContent = (String)row[MESSAGE_CONTENT_INDEX];
                LocalDateTime createdAt = (LocalDateTime)row[MESSAGE_CREATED_AT_INDEX];

                int readCount = readCountMap.getOrDefault(messageId, 0);
                int unreadCount = memberCount - readCount;
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
            responseMessages
        );
    }

    @Transactional
    public GroupChatMessageResponse sendMessage(Integer clubId, Integer userId, String content) {
        validateClubMember(clubId, userId);

        Integer roomId = getRoomIdOrThrow(clubId);
        GroupChatRoom room = entityManager.getReference(GroupChatRoom.class, roomId);
        User sender = entityManager.getReference(User.class, userId);

        GroupChatMessage message = groupChatMessageRepository.save(GroupChatMessage.of(room, sender, content));
        messageReadStatusRepository.save(MessageReadStatus.of(message, sender));

        entityManager.flush();

        Integer messageId = (Integer)entityManager.getEntityManagerFactory()
            .getPersistenceUnitUtil()
            .getIdentifier(message);

        Object[] messageRow = entityManager.createQuery(
                "SELECT m.id, m.sender.id, m.sender.name, m.content, m.createdAt "
                    + "FROM GroupChatMessage m "
                    + "WHERE m.id = :messageId",
                Object[].class
            )
            .setParameter("messageId", messageId)
            .getSingleResult();

        Integer senderId = (Integer)messageRow[SENDER_ID_INDEX];
        String senderName = (String)messageRow[SENDER_NAME_INDEX];
        String messageContent = (String)messageRow[MESSAGE_CONTENT_INDEX];
        LocalDateTime createdAt = (LocalDateTime)messageRow[MESSAGE_CREATED_AT_INDEX];

        List<Integer> recipientUserIds = getClubMemberUserIds(clubId);
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
    public void markAsRead(Integer clubId, Integer userId) {
        LocalDateTime joinedAt = getJoinedAtOrThrow(clubId, userId);
        Integer roomId = getRoomIdOrThrow(clubId);

        markAsReadInternal(roomId, userId, joinedAt);
    }

    @Transactional
    public Boolean toggleMute(Integer clubId, Integer userId) {
        validateClubMember(clubId, userId);

        Integer roomId = getRoomIdOrThrow(clubId);

        Boolean currentMuted = entityManager.createQuery(
                "SELECT s.isMuted "
                    + "FROM GroupChatNotificationSetting s "
                    + "WHERE s.room.id = :roomId "
                    + "AND s.user.id = :userId",
                Boolean.class
            )
            .setParameter("roomId", roomId)
            .setParameter("userId", userId)
            .getResultStream()
            .findFirst()
            .orElse(null);

        Boolean newMuted;

        if (currentMuted == null) {
            newMuted = true;
            GroupChatRoom roomRef = entityManager.getReference(GroupChatRoom.class, roomId);
            User userRef = entityManager.getReference(User.class, userId);
            groupChatNotificationSettingRepository.save(GroupChatNotificationSetting.of(roomRef, userRef, true));
        } else {
            newMuted = !currentMuted;
            entityManager.createQuery(
                    "UPDATE GroupChatNotificationSetting s "
                        + "SET s.isMuted = :isMuted "
                        + "WHERE s.room.id = :roomId "
                        + "AND s.user.id = :userId"
                )
                .setParameter("isMuted", newMuted)
                .setParameter("roomId", roomId)
                .setParameter("userId", userId)
                .executeUpdate();
        }

        return newMuted;
    }

    @Transactional
    public GroupChatRoom createGroupChatRoom(Club club) {
        Integer clubId = (Integer)entityManager.getEntityManagerFactory()
            .getPersistenceUnitUtil()
            .getIdentifier(club);

        return groupChatRoomRepository.findByClubId(clubId)
            .orElseGet(() -> groupChatRoomRepository.save(GroupChatRoom.of(club)));
    }

    private void validateClubMember(Integer clubId, Integer userId) {
        if (!clubMemberRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_GROUP_CHAT_ACCESS);
        }
    }

    private void markAsReadInternal(Integer roomId, Integer userId, LocalDateTime joinedAt) {
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
            .setParameter("userId", userId)
            .getResultList();

        if (unreadMessages.isEmpty()) {
            return;
        }

        List<MessageReadStatus> statuses = unreadMessages.stream()
            .map(message -> MessageReadStatus.of(message, entityManager.getReference(User.class, userId)))
            .toList();

        messageReadStatusRepository.saveAll(statuses);
    }

    private Map<Integer, Integer> getReadCountMap(List<Integer> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> counts = entityManager.createQuery(
                "SELECT rs.messageId, COUNT(rs) "
                    + "FROM MessageReadStatus rs "
                    + "WHERE rs.messageId IN :messageIds "
                    + "GROUP BY rs.messageId",
                Object[].class
            )
            .setParameter("messageIds", messageIds)
            .getResultList();

        Map<Integer, Integer> readCountMap = new HashMap<>();
        for (Object[] row : counts) {
            Integer messageId = (Integer)row[0];
            Long readCount = (Long)row[1];
            readCountMap.put(messageId, readCount.intValue());
        }

        return readCountMap;
    }

    private Set<Integer> getMutedUserIds(Integer roomId) {
        List<Integer> mutedUserIds = entityManager.createQuery(
                "SELECT s.user.id "
                    + "FROM GroupChatNotificationSetting s "
                    + "WHERE s.room.id = :roomId "
                    + "AND s.isMuted = true",
                Integer.class
            )
            .setParameter("roomId", roomId)
            .getResultList();

        return new HashSet<>(mutedUserIds);
    }

    private Integer getRoomIdOrThrow(Integer clubId) {
        return entityManager.createQuery(
                "SELECT r.id "
                    + "FROM GroupChatRoom r "
                    + "WHERE r.club.id = :clubId",
                Integer.class
            )
            .setParameter("clubId", clubId)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_GROUP_CHAT_ROOM));
    }

    private LocalDateTime getJoinedAtOrThrow(Integer clubId, Integer userId) {
        validateClubMember(clubId, userId);

        return entityManager.createQuery(
                "SELECT cm.createdAt "
                    + "FROM ClubMember cm "
                    + "WHERE cm.club.id = :clubId "
                    + "AND cm.user.id = :userId",
                LocalDateTime.class
            )
            .setParameter("clubId", clubId)
            .setParameter("userId", userId)
            .getSingleResult();
    }

    private int getMemberCount(Integer clubId) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(cm) "
                    + "FROM ClubMember cm "
                    + "WHERE cm.club.id = :clubId",
                Long.class
            )
            .setParameter("clubId", clubId)
            .getSingleResult();

        return count.intValue();
    }

    private List<Integer> getClubMemberUserIds(Integer clubId) {
        return entityManager.createQuery(
                "SELECT cm.user.id "
                    + "FROM ClubMember cm "
                    + "WHERE cm.club.id = :clubId",
                Integer.class
            )
            .setParameter("clubId", clubId)
            .getResultList();
    }

}
