package gg.agit.konect.domain.chat.unified.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.chat.direct.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.direct.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.direct.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.direct.service.ChatService;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatMuteResponse;
import gg.agit.konect.domain.chat.group.service.GroupChatService;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatMessageResponse;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatMessagesResponse;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatRoomsResponse;
import gg.agit.konect.domain.chat.unified.enums.ChatType;
import gg.agit.konect.domain.chat.unified.service.UnifiedChatService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatController implements ChatApi {

    private final ChatService chatService;
    private final GroupChatService groupChatService;
    private final UnifiedChatService unifiedChatService;

    @Override
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @Valid @RequestBody ChatRoomCreateRequest request,
        @UserId Integer userId
    ) {
        ChatRoomResponse response = chatService.createOrGetChatRoom(userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UnifiedChatRoomsResponse> getChatRooms(
        @UserId Integer userId
    ) {
        UnifiedChatRoomsResponse response = unifiedChatService.getChatRooms(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UnifiedChatMessagesResponse> getChatRoomMessages(
        @RequestParam(name = "type") ChatType type,
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    ) {
        UnifiedChatMessagesResponse response = unifiedChatService.getMessages(userId, type, chatRoomId, page, limit);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UnifiedChatMessageResponse> sendMessage(
        @RequestParam(name = "type") ChatType type,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    ) {
        UnifiedChatMessageResponse response = unifiedChatService.sendMessage(userId, type, chatRoomId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UnifiedChatMuteResponse> toggleGroupChatMute(
        @RequestParam(name = "type") ChatType type,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    ) {
        Boolean isMuted = groupChatService.toggleMute(userId, type, chatRoomId);
        return ResponseEntity.ok(new UnifiedChatMuteResponse(isMuted));
    }
}
