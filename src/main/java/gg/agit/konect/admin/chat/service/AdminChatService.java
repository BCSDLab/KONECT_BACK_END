package gg.agit.konect.admin.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.admin.chat.dto.AdminChatMessagesResponse;
import gg.agit.konect.admin.chat.dto.AdminChatMessagesResponse.InnerAdminChatMessageResponse;
import gg.agit.konect.admin.chat.dto.AdminChatRoomsResponse;
import gg.agit.konect.admin.chat.dto.AdminChatRoomsResponse.InnerAdminChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.UnreadMessageCount;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Integer userId, Integer adminId) {
        User admin = userRepository.getById(adminId);
        User targetUser = userRepository.getById(userId);

        ChatRoom chatRoom = chatRoomRepository.findByUserIdAndAdminRole(targetUser.getId(), UserRole.ADMIN)
            .orElseGet(() -> {
                ChatRoom newChatRoom = ChatRoom.of(admin, targetUser);
                return chatRoomRepository.save(newChatRoom);
            });

        return ChatRoomResponse.from(chatRoom);
    }

    public AdminChatRoomsResponse getChatRooms() {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllAdminChatRooms(UserRole.ADMIN);
        List<Integer> chatRoomIds = chatRooms.stream()
            .map(ChatRoom::getId)
            .toList();
        Map<Integer, Integer> unreadCountMap = getUnreadCountMap(chatRoomIds);

        List<InnerAdminChatRoomResponse> responses = chatRooms.stream()
            .map(chatRoom -> InnerAdminChatRoomResponse.from(
                chatRoom,
                getNormalUser(chatRoom),
                unreadCountMap.getOrDefault(chatRoom.getId(), 0)
            ))
            .toList();

        return new AdminChatRoomsResponse(responses);
    }

    private Map<Integer, Integer> getUnreadCountMap(List<Integer> chatRoomIds) {
        if (chatRoomIds.isEmpty()) {
            return Map.of();
        }

        List<UnreadMessageCount> unreadMessageCounts = chatMessageRepository.countUnreadMessagesForAdmin(
            chatRoomIds, UserRole.ADMIN
        );

        return unreadMessageCounts.stream()
            .collect(Collectors.toMap(
                UnreadMessageCount::chatRoomId,
                unreadMessageCount -> unreadMessageCount.unreadCount().intValue()
            ));
    }

    @Transactional
    public AdminChatMessagesResponse getChatRoomMessages(
        Integer chatRoomId,
        Integer page,
        Integer limit,
        Integer adminId
    ) {
        ChatRoom chatRoom = chatRoomRepository.getById(chatRoomId);
        validateAdminChatRoom(chatRoom);

        chatPresenceService.recordPresence(chatRoomId, adminId);

        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessagesForAdmin(
            chatRoomId, UserRole.ADMIN
        );
        unreadMessages.forEach(ChatMessage::markAsRead);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoomId, pageable);
        return AdminChatMessagesResponse.from(messages);
    }

    @Transactional
    public InnerAdminChatMessageResponse sendMessage(
        Integer chatRoomId,
        ChatMessageSendRequest request,
        Integer adminId
    ) {
        ChatRoom chatRoom = chatRoomRepository.getById(chatRoomId);
        validateAdminChatRoom(chatRoom);

        User admin = userRepository.getById(adminId);
        User normalUser = getNormalUser(chatRoom);

        ChatMessage chatMessage = chatMessageRepository.save(
            ChatMessage.of(chatRoom, admin, normalUser, request.content())
        );
        chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());

        notificationService.sendChatNotification(
            normalUser.getId(),
            chatRoomId,
            admin.getName(),
            request.content()
        );

        return InnerAdminChatMessageResponse.from(chatMessage);
    }

    private void validateAdminChatRoom(ChatRoom chatRoom) {
        boolean hasAdmin = chatRoom.getSender().getRole() == UserRole.ADMIN
            || chatRoom.getReceiver().getRole() == UserRole.ADMIN;
        if (!hasAdmin) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }
    }

    private User getNormalUser(ChatRoom chatRoom) {
        if (chatRoom.getSender().getRole() == UserRole.ADMIN) {
            return chatRoom.getReceiver();
        }
        return chatRoom.getSender();
    }
}
