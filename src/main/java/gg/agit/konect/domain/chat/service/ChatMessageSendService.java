package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.event.AdminChatReceivedEvent;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageSendService {

    private static final String DEFAULT_GROUP_ROOM_NAME = "그룹 채팅";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ChatRoomSystemAdminService chatRoomSystemAdminService;
    private final ChatDirectRoomAccessService chatDirectRoomAccessService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    public ChatMessageDetailResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isDirectRoom()) {
            return sendDirectMessage(userId, room, request);
        }

        if (room.isClubGroupRoom()) {
            return sendClubMessageByRoomId(room, userId, request.content());
        }

        return sendGroupMessageByRoomId(room, userId, request.content());
    }

    private ChatMessageDetailResponse sendDirectMessage(
        Integer userId,
        ChatRoom chatRoom,
        ChatMessageSendRequest request
    ) {
        Integer roomId = chatRoom.getId();
        User sender = userRepository.getById(userId);

        // 어드민이 SYSTEM_ADMIN 방에 메시지를 보내는 경우
        boolean isAdminSendingToSystemAdminRoom = sender.isAdmin()
            && chatRoomSystemAdminService.isSystemAdminRoom(chatRoom.getId());

        if (!isAdminSendingToSystemAdminRoom) {
            chatDirectRoomAccessService.getAccessibleMember(chatRoom, sender);
        }

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        User receiver = resolveDirectMessageReceiver(members, sender);

        ChatMessage chatMessage = chatMessageRepository.save(
            ChatMessage.of(chatRoom, sender, request.content())
        );

        syncLastMessage(chatRoom, chatMessage);
        members.stream()
            .filter(member -> !member.getUserId().equals(userId))
            .filter(ChatRoomMember::hasLeft)
            .forEach(member -> member.restoreDirectRoomFromIncomingMessage(chatMessage.getCreatedAt()));

        // 어드민이 보낸 경우는 lastReadAt 업데이트하지 않음 (멤버가 아니므로)
        if (!isAdminSendingToSystemAdminRoom) {
            updateLastReadAtOrEnsureMember(roomId, userId, chatMessage.getCreatedAt());
        }

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

    private ChatMessageDetailResponse sendClubMessageByRoomId(ChatRoom room, Integer userId, String content) {
        Integer roomId = room.getId();
        ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
        User sender = member.getUser();

        ensureRoomMember(room, sender, member.getCreatedAt());

        ChatMessage message = chatMessageRepository.save(ChatMessage.of(room, sender, content));
        syncLastMessage(room, message);
        updateLastReadAtOrEnsureMember(roomId, userId, message.getCreatedAt());

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

    private ChatMessageDetailResponse sendGroupMessageByRoomId(ChatRoom room, Integer userId, String content) {
        Integer roomId = room.getId();
        User sender = userRepository.getById(userId);

        ChatRoomMember senderMember = getRoomMember(roomId, userId);
        if (senderMember.hasLeft()) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.of(room, sender, content));
        syncLastMessage(room, message);
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

    private void updateLastReadAtOrEnsureMember(Integer roomId, Integer userId, LocalDateTime lastReadAt) {
        int updated = chatRoomMemberRepository.updateLastReadAtIfOlder(roomId, userId, lastReadAt);
        if (updated == 0) {
            ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
            User user = userRepository.getById(userId);
            ensureRoomMember(room, user, lastReadAt);
        }
    }

    private void updateLastReadAt(Integer roomId, Integer userId, LocalDateTime lastReadAt) {
        chatRoomMemberRepository.updateLastReadAtIfOlder(roomId, userId, lastReadAt);
    }

    private List<LocalDateTime> toSortedReadBaselines(List<ChatRoomMember> members) {
        return members.stream()
            .map(ChatRoomMember::getLastReadAt)
            .sorted()
            .toList();
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

    private void syncLastMessage(ChatRoom room, ChatMessage message) {
        // 채팅방 목록은 chat_room.last_message_*를 직접 조회하므로
        // 동시 전송에서도 가장 최신 메시지만 메타데이터를 덮어쓰도록 DB 조건을 같이 건다.
        int updated = chatRoomRepository.updateLastMessageIfLatest(
            room.getId(),
            message.getId(),
            message.getContent(),
            message.getCreatedAt()
        );
        if (updated > 0) {
            room.updateLastMessage(message.getContent(), message.getCreatedAt());
        }
    }

    private void publishAdminChatEventIfNeeded(boolean isSystemAdminRoom, User sender, String content) {
        if (isSystemAdminRoom && !sender.isAdmin()) {
            eventPublisher.publishEvent(AdminChatReceivedEvent.of(sender.getId(), sender.getName(), content));
        }
    }

    private User resolveDirectMessageReceiver(List<ChatRoomMember> members, User sender) {
        Map<Integer, User> userMap = members.stream()
            .collect(Collectors.toMap(
                ChatRoomMember::getUserId,
                ChatRoomMember::getUser,
                (existing, replacement) -> existing
            ));
        List<MemberInfo> memberInfos = members.stream()
            .map(member -> new MemberInfo(member.getUserId(), member.getCreatedAt()))
            .toList();
        return resolveMessageReceiverFromMemberInfo(sender, memberInfos, userMap);
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

    private User findNonAdminUserFromMemberInfo(List<MemberInfo> memberInfos, Map<Integer, User> userMap) {
        return memberInfos.stream()
            .sorted(Comparator.comparing(MemberInfo::createdAt))
            .map(info -> userMap.get(info.userId()))
            .filter(user -> user != null && !user.isAdmin())
            .findFirst()
            .orElse(null);
    }

    private User resolveMessageReceiverFromMemberInfo(
        User sender,
        List<MemberInfo> memberInfos,
        Map<Integer, User> userMap
    ) {
        if (sender.isAdmin()) {
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

    private record MemberInfo(Integer userId, LocalDateTime createdAt) {
    }
}
