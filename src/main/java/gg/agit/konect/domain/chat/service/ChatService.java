package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.INVALID_CHAT_ROOM_CREATE_REQUEST;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_PRESIDENT;

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
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse.InnerChatRoomResponse;
import gg.agit.konect.domain.chat.dto.UnreadMessageCount;
import gg.agit.konect.domain.chat.enums.ChatRoomType;
import gg.agit.konect.domain.chat.event.AdminChatReceivedEvent;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
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
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Integer userId, ChatRoomCreateRequest request) {
        validateChatRoomCreateRequest(request);

        User currentUser = userRepository.getById(userId);
        User targetUser = resolveTargetUser(request);

        if (currentUser.getId().equals(targetUser.getId())) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }

        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(currentUser.getId(), targetUser.getId())
            .orElseGet(() -> {
                ChatRoom newChatRoom = ChatRoom.of(currentUser, targetUser);
                return chatRoomRepository.save(newChatRoom);
            });

        return ChatRoomResponse.from(chatRoom);
    }

    private void validateChatRoomCreateRequest(ChatRoomCreateRequest request) {
        boolean hasClubId = request.hasClubId();
        boolean hasTargetUserId = request.hasTargetUserId();

        if (hasClubId == hasTargetUserId) {
            throw CustomException.of(INVALID_CHAT_ROOM_CREATE_REQUEST);
        }
    }

    private User resolveTargetUser(ChatRoomCreateRequest request) {
        if (request.hasClubId()) {
            ClubMember clubPresident = clubMemberRepository.findPresidentByClubId(request.clubId())
                .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_PRESIDENT));
            return clubPresident.getUser();
        }
        return userRepository.getById(request.targetUserId());
    }

    public ChatRoomsResponse getChatRooms(Integer userId) {
        User user = userRepository.getById(userId);
        List<InnerChatRoomResponse> chatRoomResponses = new ArrayList<>();

        List<ChatRoom> personalChatRooms = chatRoomRepository.findByUserId(userId);
        Map<Integer, Integer> personalUnreadCountMap = getUnreadCountMap(
            extractChatRoomIds(personalChatRooms), userId
        );

        Set<Integer> addedChatRoomIds = new HashSet<>();
        if (user.getRole() == UserRole.ADMIN) {
            List<ChatRoom> adminChatRooms = chatRoomRepository.findAllAdminChatRooms(UserRole.ADMIN);
            Map<Integer, Integer> adminUnreadCountMap = getAdminUnreadCountMap(
                extractChatRoomIds(adminChatRooms)
            );

            for (ChatRoom chatRoom : adminChatRooms) {
                chatRoomResponses.add(InnerChatRoomResponse.from(
                    chatRoom, user, adminUnreadCountMap, ChatRoomType.ADMIN
                ));
                addedChatRoomIds.add(chatRoom.getId());
            }
        }

        for (ChatRoom chatRoom : personalChatRooms) {
            if (!addedChatRoomIds.contains(chatRoom.getId())) {
                chatRoomResponses.add(InnerChatRoomResponse.from(
                    chatRoom, user, personalUnreadCountMap, ChatRoomType.NORMAL
                ));
            }
        }

        chatRoomResponses.sort(Comparator
            .comparing(InnerChatRoomResponse::lastSentTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(InnerChatRoomResponse::chatRoomId));

        return ChatRoomsResponse.of(chatRoomResponses);
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
            chatRoomIds, userId
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
            chatRoomIds, UserRole.ADMIN
        );

        return unreadMessageCounts.stream()
            .collect(Collectors.toMap(
                UnreadMessageCount::chatRoomId,
                unreadMessageCount -> unreadMessageCount.unreadCount().intValue()
            ));
    }

    @Transactional
    public ChatMessagesResponse getChatRoomMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        ChatRoom chatRoom = chatRoomRepository.getById(roomId);
        chatRoom.validateIsParticipant(userId);

        chatPresenceService.recordPresence(roomId, userId);
        markUnreadMessagesAsRead(userId, roomId, chatRoom);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, pageable);

        Integer maskedAdminId = getMaskedAdminId(userId, chatRoom);
        return ChatMessagesResponse.from(messages, userId, maskedAdminId);
    }

    private Integer getMaskedAdminId(Integer userId, ChatRoom chatRoom) {
        User user = userRepository.getById(userId);
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

    private void markUnreadMessagesAsRead(Integer userId, Integer roomId, ChatRoom chatRoom) {
        User user = userRepository.getById(userId);
        List<ChatMessage> unreadMessages;

        if (user.getRole() == UserRole.ADMIN && isAdminChatRoom(chatRoom)) {
            unreadMessages = chatMessageRepository.findUnreadMessagesForAdmin(roomId, UserRole.ADMIN);
        } else {
            unreadMessages = chatMessageRepository.findUnreadMessagesByChatRoomIdAndUserId(roomId, userId);
        }

        unreadMessages.forEach(ChatMessage::markAsRead);
    }

    private boolean isAdminChatRoom(ChatRoom chatRoom) {
        return chatRoom.getSender().getRole() == UserRole.ADMIN
            || chatRoom.getReceiver().getRole() == UserRole.ADMIN;
    }

    @Transactional
    public ChatMessageResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatRoom chatRoom = chatRoomRepository.getById(roomId);
        chatRoom.validateIsParticipant(userId);

        User sender = userRepository.getById(userId);
        User receiver = chatRoom.getChatPartner(sender);

        ChatMessage chatMessage = chatMessageRepository.save(
            ChatMessage.of(chatRoom, sender, receiver, request.content())
        );
        chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());

        notificationService.sendChatNotification(receiver.getId(), roomId, sender.getName(), request.content());
        publishAdminChatEventIfNeeded(receiver, sender, request.content());

        return ChatMessageResponse.from(chatMessage, userId);
    }

    private void publishAdminChatEventIfNeeded(User receiver, User sender, String content) {
        if (receiver.getRole() == UserRole.ADMIN) {
            eventPublisher.publishEvent(AdminChatReceivedEvent.of(sender.getId(), sender.getName(), content));
        }
    }
}
