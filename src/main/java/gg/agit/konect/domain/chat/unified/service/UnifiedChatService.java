package gg.agit.konect.domain.chat.unified.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.chat.direct.dto.ChatMessageResponse;
import gg.agit.konect.domain.chat.direct.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.direct.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.direct.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.direct.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.direct.dto.ChatRoomsResponse;
import gg.agit.konect.domain.chat.direct.model.ChatRoom;
import gg.agit.konect.domain.chat.direct.service.ChatService;
import gg.agit.konect.domain.chat.group.dto.GroupChatMessageResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatMessagesResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatRoomsResponse;
import gg.agit.konect.domain.chat.group.service.GroupChatService;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatMessageResponse;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatMessagesResponse;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatRoomResponse;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatRoomsResponse;
import gg.agit.konect.domain.chat.unified.enums.ChatType;
import gg.agit.konect.domain.chat.direct.repository.ChatRoomRepository;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.global.exception.CustomException;

@Service
public class UnifiedChatService {

    private final ChatService chatService;
    private final GroupChatService groupChatService;
    private final ChatRoomRepository chatRoomRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;

    public UnifiedChatService(
        ChatService chatService,
        GroupChatService groupChatService,
        ChatRoomRepository chatRoomRepository,
        NotificationMuteSettingRepository notificationMuteSettingRepository
    ) {
        this.chatService = chatService;
        this.groupChatService = groupChatService;
        this.chatRoomRepository = chatRoomRepository;
        this.notificationMuteSettingRepository = notificationMuteSettingRepository;
    }

    public ChatRoomResponse createOrGetChatRoom(Integer userId, ChatRoomCreateRequest request) {
        return chatService.createOrGetChatRoom(userId, request);
    }

    public UnifiedChatRoomsResponse getChatRooms(Integer userId) {
        ChatRoomsResponse directRooms = chatService.getChatRooms(userId);
        GroupChatRoomsResponse groupRooms = groupChatService.getChatRooms(userId);

        List<Integer> roomIds = new ArrayList<>();
        roomIds.addAll(
            directRooms.chatRooms().stream().map(ChatRoomsResponse.InnerChatRoomResponse::chatRoomId).toList()
        );
        roomIds.addAll(
            groupRooms.groupChatRooms().stream()
                .map(GroupChatRoomsResponse.InnerGroupChatRoomResponse::groupChatRoomId)
                .toList()
        );

        Map<Integer, Boolean> muteMap = getMuteMap(roomIds, userId);
        List<UnifiedChatRoomResponse> rooms = new ArrayList<>();

        directRooms.chatRooms().forEach(room -> rooms.add(new UnifiedChatRoomResponse(
            room.chatRoomId(),
            ChatType.DIRECT,
            room.chatPartnerName(),
            room.chatPartnerProfileImage(),
            room.lastMessage(),
            room.lastSentTime(),
            room.unreadCount(),
            muteMap.getOrDefault(room.chatRoomId(), false)
        )));

        groupRooms.groupChatRooms().forEach(room -> rooms.add(new UnifiedChatRoomResponse(
            room.groupChatRoomId(),
            ChatType.GROUP,
            room.clubName(),
            room.clubImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.unreadMessageCount(),
            muteMap.getOrDefault(room.groupChatRoomId(), false)
        )));

        rooms.sort(
            Comparator.comparing(UnifiedChatRoomResponse::lastSentAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UnifiedChatRoomResponse::roomId)
        );

        return new UnifiedChatRoomsResponse(rooms);
    }

    public UnifiedChatMessagesResponse getMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isDirectRoom()) {
            return toUnifiedResponse(chatService.getChatRoomMessages(userId, roomId, page, limit));
        }

        return toUnifiedResponse(groupChatService.getMessagesByRoomId(roomId, userId, page, limit));
    }

    public UnifiedChatMessageResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isDirectRoom()) {
            ChatMessageResponse response = chatService.sendMessage(userId, roomId, request);
            return new UnifiedChatMessageResponse(
                response.messageId(),
                response.senderId(),
                null,
                response.content(),
                response.createdAt(),
                response.isRead(),
                null,
                response.isMine()
            );
        }

        GroupChatMessageResponse response = groupChatService.sendMessageByRoomId(roomId, userId, request.content());
        return new UnifiedChatMessageResponse(
            response.messageId(),
            response.senderId(),
            response.senderName(),
            response.content(),
            response.createdAt(),
            null,
            response.unreadCount(),
            response.isMine()
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

    private UnifiedChatMessagesResponse toUnifiedResponse(ChatMessagesResponse response) {
        List<UnifiedChatMessageResponse> messages = response.messages().stream()
            .map(message -> new UnifiedChatMessageResponse(
                message.messageId(),
                message.senderId(),
                null,
                message.content(),
                message.createdAt(),
                message.isRead(),
                null,
                message.isMine()
            ))
            .toList();

        return new UnifiedChatMessagesResponse(
            response.totalCount(),
            response.currentCount(),
            response.totalPage(),
            response.currentPage(),
            null,
            messages
        );
    }

    private UnifiedChatMessagesResponse toUnifiedResponse(GroupChatMessagesResponse response) {
        List<UnifiedChatMessageResponse> messages = response.messages().stream()
            .map(message -> new UnifiedChatMessageResponse(
                message.messageId(),
                message.senderId(),
                message.senderName(),
                message.content(),
                message.createdAt(),
                null,
                message.unreadCount(),
                message.isMine()
            ))
            .toList();

        return new UnifiedChatMessagesResponse(
            response.totalCount(),
            response.currentCount(),
            response.totalPage(),
            response.currentPage(),
            response.clubId(),
            messages
        );
    }
}
