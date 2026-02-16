package gg.agit.konect.domain.chat.unified.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.chat.direct.dto.ChatMessageResponse;
import gg.agit.konect.domain.chat.direct.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.direct.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.direct.dto.ChatRoomsResponse;
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
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UnifiedChatService {

    private final ChatService chatService;
    private final GroupChatService groupChatService;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;

    public UnifiedChatRoomsResponse getChatRooms(Integer userId) {
        ChatRoomsResponse directRooms = chatService.getChatRooms(userId);
        GroupChatRoomsResponse groupRooms = groupChatService.getChatRooms(userId);
        Map<Integer, Boolean> directMuteMap = getMuteMap(
            NotificationTargetType.DIRECT_CHAT_ROOM,
            directRooms.chatRooms().stream()
                .map(ChatRoomsResponse.InnerChatRoomResponse::chatRoomId)
                .toList(),
            userId
        );
        Map<Integer, Boolean> groupMuteMap = getMuteMap(
            NotificationTargetType.GROUP_CHAT_ROOM,
            groupRooms.groupChatRooms().stream()
                .map(GroupChatRoomsResponse.InnerGroupChatRoomResponse::groupChatRoomId)
                .toList(),
            userId
        );

        List<UnifiedChatRoomResponse> rooms = new ArrayList<>();

        directRooms.chatRooms().forEach(room -> rooms.add(new UnifiedChatRoomResponse(
            room.chatRoomId(),
            ChatType.DIRECT,
            room.chatPartnerName(),
            room.chatPartnerProfileImage(),
            room.lastMessage(),
            room.lastSentTime(),
            room.unreadCount(),
            directMuteMap.getOrDefault(room.chatRoomId(), false)
        )));

        groupRooms.groupChatRooms().forEach(room -> rooms.add(new UnifiedChatRoomResponse(
            room.groupChatRoomId(),
            ChatType.GROUP,
            room.clubName(),
            room.clubImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.unreadMessageCount(),
            groupMuteMap.getOrDefault(room.groupChatRoomId(), false)
        )));

        rooms.sort(
            Comparator.comparing(UnifiedChatRoomResponse::lastSentAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UnifiedChatRoomResponse::roomId)
        );

        return new UnifiedChatRoomsResponse(rooms);
    }

    private Map<Integer, Boolean> getMuteMap(
        NotificationTargetType targetType,
        List<Integer> roomIds,
        Integer userId
    ) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        List<NotificationMuteSetting> settings = notificationMuteSettingRepository
            .findByTargetTypeAndTargetIdsAndUserId(targetType, roomIds, userId);

        Map<Integer, Boolean> muteMap = new HashMap<>();
        for (NotificationMuteSetting setting : settings) {
            Integer targetId = setting.getTargetId();
            if (targetId != null) {
                muteMap.put(targetId, setting.getIsMuted());
            }
        }

        return muteMap;
    }

    public UnifiedChatMessagesResponse getMessages(
        Integer userId,
        ChatType chatType,
        Integer roomId,
        Integer page,
        Integer limit
    ) {
        if (chatType == ChatType.DIRECT) {
            ChatMessagesResponse response = chatService.getChatRoomMessages(userId, roomId, page, limit);
            return toUnifiedResponse(response);
        }

        GroupChatMessagesResponse response = groupChatService.getMessagesByRoomId(roomId, userId, page, limit);
        return toUnifiedResponse(response);
    }

    public UnifiedChatMessageResponse sendMessage(
        Integer userId,
        ChatType chatType,
        Integer roomId,
        ChatMessageSendRequest request
    ) {
        if (chatType == ChatType.DIRECT) {
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
