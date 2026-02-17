package gg.agit.konect.domain.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ChatMuteResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomListResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.service.ChatCoordinatorService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatController implements ChatApi {

    private final ChatCoordinatorService chatCoordinatorService;

    @Override
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @Valid @RequestBody ChatRoomCreateRequest request,
        @UserId Integer userId
    ) {
        ChatRoomResponse response = chatCoordinatorService.createOrGetChatRoom(userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatRoomListResponse> getChatRooms(
        @UserId Integer userId
    ) {
        ChatRoomListResponse response = chatCoordinatorService.getChatRooms(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatMessagePageResponse> getChatRoomMessages(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    ) {
        ChatMessagePageResponse response = chatCoordinatorService.getMessages(userId, chatRoomId, page, limit);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatMessageDetailResponse> sendMessage(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    ) {
        ChatMessageDetailResponse response = chatCoordinatorService.sendMessage(userId, chatRoomId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatMuteResponse> toggleChatMute(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    ) {
        return ResponseEntity.ok(chatCoordinatorService.toggleMute(userId, chatRoomId));
    }
}
