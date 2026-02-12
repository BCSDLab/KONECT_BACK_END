package gg.agit.konect.domain.groupchat.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
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
import gg.agit.konect.domain.groupchat.model.GroupChatReadStatus;
import gg.agit.konect.domain.groupchat.model.GroupChatRoom;
import gg.agit.konect.domain.groupchat.repository.GroupChatMessageRepository;
import gg.agit.konect.domain.groupchat.repository.GroupChatNotificationSettingRepository;
import gg.agit.konect.domain.groupchat.repository.GroupChatReadStatusRepository;
import gg.agit.konect.domain.groupchat.repository.GroupChatRoomRepository;
import gg.agit.konect.domain.notification.service.NotificationService;
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
    private final GroupChatNotificationSettingRepository groupChatNotificationSettingRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;

    public GroupChatRoomResponse getGroupChatRoom(Integer clubId, Integer userId) {
        validateClubMember(clubId, userId);

        Integer roomId = groupChatRoomRepository.getIdByClubId(clubId);
        return new GroupChatRoomResponse(roomId);
    }

    @Transactional
    public GroupChatMessagesResponse getMessages(Integer clubId, Integer userId, Integer page, Integer limit) {
        validateClubMember(clubId, userId);

        LocalDateTime joinedAt = clubMemberRepository.getJoinedAtByClubIdAndUserId(clubId, userId);
        Integer roomId = groupChatRoomRepository.getIdByClubId(clubId);

        chatPresenceService.recordPresence(roomId, userId);

        markAsReadInternal(roomId, userId, LocalDateTime.now());

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
            responseMessages
        );
    }

    @Transactional
    public GroupChatMessageResponse sendMessage(Integer clubId, Integer userId, String content) {
        validateClubMember(clubId, userId);

        Integer roomId = groupChatRoomRepository.getIdByClubId(clubId);
        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);
        User sender = userRepository.getById(userId);

        GroupChatMessage message = groupChatMessageRepository.save(GroupChatMessage.of(room, sender, content));
        markAsReadInternal(roomId, userId, message.getCreatedAt());

        Integer messageId = message.getId();
        Integer senderId = sender.getId();
        String senderName = sender.getName();
        String messageContent = message.getContent();
        LocalDateTime createdAt = message.getCreatedAt();

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
        Integer roomId = groupChatRoomRepository.getIdByClubId(clubId);

        markAsReadInternal(roomId, userId, LocalDateTime.now());
    }

    @Transactional
    public Boolean toggleMute(Integer clubId, Integer userId) {
        validateClubMember(clubId, userId);

        Integer roomId = groupChatRoomRepository.getIdByClubId(clubId);

        GroupChatRoom room = groupChatRoomRepository.getByClubId(clubId);
        User user = userRepository.getById(userId);

        return groupChatNotificationSettingRepository.findByRoomIdAndUserId(roomId, userId)
            .map(setting -> {
                setting.toggleMute();
                groupChatNotificationSettingRepository.save(setting);
                return setting.getIsMuted();
            })
            .orElseGet(() -> {
                groupChatNotificationSettingRepository.save(GroupChatNotificationSetting.of(room, user, true));
                return true;
            });
    }

    @Transactional
    public GroupChatRoom createGroupChatRoom(Club club) {
        Integer clubId = club.getId();

        return groupChatRoomRepository.findByClubId(clubId)
            .orElseGet(() -> groupChatRoomRepository.save(GroupChatRoom.of(club)));
    }

    private void validateClubMember(Integer clubId, Integer userId) {
        if (!clubMemberRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_GROUP_CHAT_ACCESS);
        }
    }

    private void markAsReadInternal(Integer roomId, Integer userId, LocalDateTime lastReadAt) {
        GroupChatRoom room = groupChatRoomRepository.getById(roomId);
        User user = userRepository.getById(userId);
        groupChatReadStatusRepository.findByRoomIdAndUserId(roomId, userId)
            .ifPresentOrElse(status -> {
                status.updateLastReadAt(lastReadAt);
                groupChatReadStatusRepository.save(status);
            }, () -> groupChatReadStatusRepository.save(GroupChatReadStatus.of(room, user, lastReadAt)));
    }

    private Set<Integer> getMutedUserIds(Integer roomId) {
        List<Integer> mutedUserIds = groupChatNotificationSettingRepository.findByRoomIdAndIsMutedTrue(roomId).stream()
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

    private List<Integer> getClubMemberUserIds(Integer clubId) {
        return clubMemberRepository.findUserIdsByClubId(clubId);
    }

}
