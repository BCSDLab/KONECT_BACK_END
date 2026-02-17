package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.ChatMuteResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomListResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomSummaryResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.domain.chat.dto.ClubMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ClubMessageResponse;
import gg.agit.konect.domain.chat.dto.ClubRoomListResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.global.exception.CustomException;

@Service
public class ChatCoordinatorService {

    private final ChatService chatService;
    private final ClubChatService clubChatService;
    private final ChatRoomRepository chatRoomRepository;
    private final NotificationMuteSettingRepository notificationMuteSettingRepository;

    public ChatCoordinatorService(
        ChatService chatService,
        ClubChatService clubChatService,
        ChatRoomRepository chatRoomRepository,
        NotificationMuteSettingRepository notificationMuteSettingRepository
    ) {
        this.chatService = chatService;
        this.clubChatService = clubChatService;
        this.chatRoomRepository = chatRoomRepository;
        this.notificationMuteSettingRepository = notificationMuteSettingRepository;
    }

    public ChatRoomResponse createOrGetChatRoom(Integer userId, ChatRoomCreateRequest request) {
        return chatService.createOrGetChatRoom(userId, request);
    }

    public ChatRoomListResponse getChatRooms(Integer userId) {
        ChatRoomsResponse directRooms = chatService.getChatRooms(userId);
        ClubRoomListResponse clubRooms = clubChatService.getChatRooms(userId);

        List<Integer> roomIds = new ArrayList<>();
        roomIds.addAll(
            directRooms.chatRooms().stream().map(ChatRoomsResponse.InnerChatRoomResponse::chatRoomId).toList()
        );
        roomIds.addAll(clubRooms.clubRooms().stream().map(ClubRoomListResponse.InnerClubRoomResponse::roomId).toList());

        Map<Integer, Boolean> muteMap = getMuteMap(roomIds, userId);
        List<ChatRoomSummaryResponse> rooms = new ArrayList<>();

        directRooms.chatRooms().forEach(room -> rooms.add(new ChatRoomSummaryResponse(
            room.chatRoomId(),
            ChatType.DIRECT,
            room.chatPartnerName(),
            room.chatPartnerProfileImage(),
            room.lastMessage(),
            room.lastSentTime(),
            room.unreadCount(),
            muteMap.getOrDefault(room.chatRoomId(), false)
        )));

        clubRooms.clubRooms().forEach(room -> rooms.add(new ChatRoomSummaryResponse(
            room.roomId(),
            ChatType.GROUP,
            room.clubName(),
            room.clubImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.unreadMessageCount(),
            muteMap.getOrDefault(room.roomId(), false)
        )));

        rooms.sort(
            Comparator.comparing(ChatRoomSummaryResponse::lastSentAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ChatRoomSummaryResponse::roomId)
        );

        return new ChatRoomListResponse(rooms);
    }

    public ChatMessagePageResponse getMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isDirectRoom()) {
            return toMessagePageResponse(chatService.getChatRoomMessages(userId, roomId, page, limit));
        }

        return toMessagePageResponse(clubChatService.getMessagesByRoomId(roomId, userId, page, limit));
    }

    public ChatMessageDetailResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isDirectRoom()) {
            ChatMessageResponse response = chatService.sendMessage(userId, roomId, request);
            return new ChatMessageDetailResponse(
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

        ClubMessageResponse response = clubChatService.sendMessageByRoomId(roomId, userId, request.content());
        return new ChatMessageDetailResponse(
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

    public ChatMuteResponse toggleMute(Integer userId, Integer roomId) {
        return new ChatMuteResponse(clubChatService.toggleMute(userId, roomId));
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

    private ChatMessagePageResponse toMessagePageResponse(ChatMessagesResponse response) {
        List<ChatMessageDetailResponse> messages = response.messages().stream()
            .map(message -> new ChatMessageDetailResponse(
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

        return new ChatMessagePageResponse(
            response.totalCount(),
            response.currentCount(),
            response.totalPage(),
            response.currentPage(),
            null,
            messages
        );
    }

    private ChatMessagePageResponse toMessagePageResponse(ClubMessagePageResponse response) {
        List<ChatMessageDetailResponse> messages = response.messages().stream()
            .map(message -> new ChatMessageDetailResponse(
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

        return new ChatMessagePageResponse(
            response.totalCount(),
            response.currentCount(),
            response.totalPage(),
            response.currentPage(),
            response.clubId(),
            messages
        );
    }
}
