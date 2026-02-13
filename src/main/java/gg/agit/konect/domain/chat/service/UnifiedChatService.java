package gg.agit.konect.domain.chat.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.chat.dto.ChatMessageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.domain.chat.dto.UnifiedChatMessageResponse;
import gg.agit.konect.domain.chat.dto.UnifiedChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.UnifiedChatRoomResponse;
import gg.agit.konect.domain.chat.dto.UnifiedChatRoomsResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.groupchat.dto.GroupChatMessageResponse;
import gg.agit.konect.domain.groupchat.dto.GroupChatMessagesResponse;
import gg.agit.konect.domain.groupchat.dto.GroupChatRoomsResponse;
import gg.agit.konect.domain.groupchat.model.GroupChatRoom;
import gg.agit.konect.domain.groupchat.repository.GroupChatRoomRepository;
import gg.agit.konect.domain.groupchat.service.GroupChatService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UnifiedChatService {

    private final ChatService chatService;
    private final GroupChatService groupChatService;
    private final ChatRoomRepository chatRoomRepository;
    private final GroupChatRoomRepository groupChatRoomRepository;
    private final ClubMemberRepository clubMemberRepository;

    public UnifiedChatRoomsResponse getChatRooms(Integer userId) {
        ChatRoomsResponse directRooms = chatService.getChatRooms(userId);
        GroupChatRoomsResponse groupRooms = groupChatService.getChatRooms(userId);

        List<UnifiedChatRoomResponse> rooms = new ArrayList<>();

        directRooms.chatRooms().forEach(room -> rooms.add(new UnifiedChatRoomResponse(
            room.chatRoomId(),
            ChatType.DIRECT,
            room.chatPartnerName(),
            room.chatPartnerProfileImage(),
            room.lastMessage(),
            room.lastSentTime(),
            room.unreadCount()
        )));

        groupRooms.groupChatRooms().forEach(room -> rooms.add(new UnifiedChatRoomResponse(
            room.groupChatRoomId(),
            ChatType.GROUP,
            room.clubName(),
            room.clubImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.unreadMessageCount()
        )));

        rooms.sort(
            Comparator.comparing(UnifiedChatRoomResponse::lastSentAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UnifiedChatRoomResponse::roomId)
        );

        return new UnifiedChatRoomsResponse(rooms);
    }

    public UnifiedChatMessagesResponse getMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        ChatType chatType = resolveChatType(userId, roomId);

        if (chatType == ChatType.DIRECT) {
            ChatMessagesResponse response = chatService.getChatRoomMessages(userId, roomId, page, limit);
            return toUnifiedResponse(response);
        }

        GroupChatMessagesResponse response = groupChatService.getMessagesByRoomId(roomId, userId, page, limit);
        return toUnifiedResponse(response);
    }

    public UnifiedChatMessageResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatType chatType = resolveChatType(userId, roomId);

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

    public void markAsRead(Integer userId, Integer roomId) {
        ChatType chatType = resolveChatType(userId, roomId);

        if (chatType == ChatType.DIRECT) {
            chatService.markAsRead(roomId, userId);
            return;
        }

        groupChatService.markAsReadByRoomId(roomId, userId);
    }

    private ChatType resolveChatType(Integer userId, Integer roomId) {
        ChatRoom directRoom = chatRoomRepository.findById(roomId)
            .orElse(null);
        boolean canAccessDirect = directRoom != null && directRoom.isParticipant(userId);

        GroupChatRoom groupRoom = groupChatRoomRepository.findById(roomId)
            .orElse(null);
        boolean canAccessGroup = groupRoom != null
            && clubMemberRepository.existsByClubIdAndUserId(groupRoom.getClub().getId(), userId);

        if (canAccessDirect && canAccessGroup) {
            throw CustomException.of(ApiResponseCode.ILLEGAL_STATE);
        }

        if (canAccessDirect) {
            return ChatType.DIRECT;
        }

        if (canAccessGroup) {
            return ChatType.GROUP;
        }

        if (directRoom != null) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        if (groupRoom != null) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_GROUP_CHAT_ACCESS);
        }

        throw CustomException.of(ApiResponseCode.ILLEGAL_ARGUMENT);
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
            messages
        );
    }
}
