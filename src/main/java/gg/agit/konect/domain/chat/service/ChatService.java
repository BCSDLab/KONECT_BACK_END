package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ChatMessageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.domain.chat.dto.UnreadMessageCount;
import gg.agit.konect.domain.chat.enums.ChatRoomType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.event.AdminChatReceivedEvent;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
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

        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(currentUser.getId(), targetUser.getId())
            .orElseGet(() -> chatRoomRepository.save(ChatRoom.of(currentUser, targetUser)));

        LocalDateTime joinedAt = chatRoom.getCreatedAt() != null ? chatRoom.getCreatedAt() : LocalDateTime.now();
        ensureRoomMember(chatRoom, currentUser, joinedAt);
        ensureRoomMember(chatRoom, targetUser, joinedAt);

        return ChatRoomResponse.from(chatRoom);
    }

    public ChatRoomsResponse getChatRooms(Integer userId) {
        User user = userRepository.getById(userId);
        List<ChatRoomsResponse.InnerChatRoomResponse> chatRoomResponses = new ArrayList<>();

        List<ChatRoom> personalChatRooms = chatRoomRepository.findByUserId(userId);
        Map<Integer, Integer> personalUnreadCountMap = getUnreadCountMap(extractChatRoomIds(personalChatRooms), userId);

        Set<Integer> addedChatRoomIds = new HashSet<>();
        if (user.getRole() == UserRole.ADMIN) {
            List<ChatRoom> adminChatRooms = chatRoomRepository.findAdminChatRoomsWithUserReply(UserRole.ADMIN);
            Map<Integer, Integer> adminUnreadCountMap = getAdminUnreadCountMap(extractChatRoomIds(adminChatRooms));

            for (ChatRoom chatRoom : adminChatRooms) {
                chatRoomResponses.add(
                    ChatRoomsResponse.InnerChatRoomResponse.fromForAdmin(chatRoom, adminUnreadCountMap)
                );
                addedChatRoomIds.add(chatRoom.getId());
            }
        }

        for (ChatRoom chatRoom : personalChatRooms) {
            if (!addedChatRoomIds.contains(chatRoom.getId())) {
                ChatRoomType type = isAdminChatRoom(chatRoom) ? ChatRoomType.ADMIN : ChatRoomType.NORMAL;
                chatRoomResponses.add(
                    ChatRoomsResponse.InnerChatRoomResponse.from(chatRoom, user, personalUnreadCountMap, type)
                );
            }
        }

        chatRoomResponses.sort(Comparator
            .comparing(
                ChatRoomsResponse.InnerChatRoomResponse::lastSentTime,
                Comparator.nullsLast(Comparator.reverseOrder())
            )
            .thenComparing(ChatRoomsResponse.InnerChatRoomResponse::chatRoomId));

        return ChatRoomsResponse.of(chatRoomResponses);
    }

    @Transactional
    public ChatMessagesResponse getChatRoomMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        ChatRoom chatRoom = getDirectRoom(roomId);
        User user = userRepository.getById(userId);
        validateChatRoomAccess(userId, chatRoom);

        ChatRoomMember member = getRoomMember(roomId, userId);
        LocalDateTime readAt = LocalDateTime.now();
        chatPresenceService.recordPresence(roomId, userId);
        member.updateLastReadAt(readAt);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, pageable);

        Integer maskedAdminId = getMaskedAdminId(user, chatRoom);
        return ChatMessagesResponse.from(messages, userId, maskedAdminId, readAt);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatRoom chatRoom = getDirectRoom(roomId);
        User sender = userRepository.getById(userId);
        validateChatRoomAccess(userId, chatRoom);

        User receiver = getMessageReceiver(sender, chatRoom);

        ChatMessage chatMessage = chatMessageRepository.save(
            ChatMessage.of(chatRoom, sender, request.content())
        );
        chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());
        updateMemberLastReadAt(roomId, userId, chatMessage.getCreatedAt());

        notificationService.sendChatNotification(receiver.getId(), roomId, sender.getName(), request.content());
        publishAdminChatEventIfNeeded(receiver, sender, request.content());

        return ChatMessageResponse.from(chatMessage, userId);
    }

    private ChatRoom getDirectRoom(Integer roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (!chatRoom.isDirectRoom()) {
            throw CustomException.of(NOT_FOUND_CHAT_ROOM);
        }

        return chatRoom;
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
        if (user.getRole() == UserRole.ADMIN || !isAdminChatRoom(chatRoom)) {
            return null;
        }
        return getAdminFromChatRoom(chatRoom).getId();
    }

    private User getAdminFromChatRoom(ChatRoom chatRoom) {
        if (chatRoom.getSender().getRole() == UserRole.ADMIN) {
            return chatRoom.getSender();
        }
        return chatRoom.getReceiver();
    }

    private boolean isAdminChatRoom(ChatRoom chatRoom) {
        return chatRoom.getSender().getRole() == UserRole.ADMIN
            || chatRoom.getReceiver().getRole() == UserRole.ADMIN;
    }

    private void validateChatRoomAccess(Integer userId, ChatRoom chatRoom) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoom.getId(), userId)) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }
    }

    private User getMessageReceiver(User sender, ChatRoom chatRoom) {
        if (sender.getRole() == UserRole.ADMIN && isAdminChatRoom(chatRoom)) {
            return chatRoom.getNonAdminUser();
        }
        return chatRoom.getChatPartner(sender);
    }

    private void publishAdminChatEventIfNeeded(User receiver, User sender, String content) {
        if (receiver.getRole() == UserRole.ADMIN) {
            eventPublisher.publishEvent(AdminChatReceivedEvent.of(sender.getId(), sender.getName(), content));
        }
    }

    private ChatRoomMember getRoomMember(Integer roomId, Integer userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
            .orElseThrow(() -> CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS));
    }

    private void ensureRoomMember(ChatRoom room, User user, LocalDateTime lastReadAt) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> member.updateLastReadAt(lastReadAt), () -> {
                chatRoomMemberRepository.save(ChatRoomMember.of(room, user, lastReadAt));
            });
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
}
