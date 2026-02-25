package gg.agit.konect.domain.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ChatMuteResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsSummaryResponse;
import gg.agit.konect.domain.chat.service.ChatService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
@Validated
public class ChatController implements ChatApi {

    private final ChatService chatService;

    @Override
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @Valid @RequestBody ChatRoomCreateRequest request,
        @UserId Integer userId
    ) {
        ChatRoomResponse response = chatService.createOrGetChatRoom(userId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatRoomResponse> createOrGetAdminChatRoom(
        @UserId Integer userId
    ) {
        ChatRoomResponse response = chatService.createOrGetAdminChatRoom(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatRoomsSummaryResponse> getChatRooms(
        @UserId Integer userId
    ) {
        ChatRoomsSummaryResponse response = chatService.getChatRooms(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatMessagePageResponse> getChatRoomMessages(
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    ) {
        ChatMessagePageResponse response = chatService.getMessages(userId, chatRoomId, page, limit);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatMessageDetailResponse> sendMessage(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    ) {
        ChatMessageDetailResponse response = chatService.sendMessage(userId, chatRoomId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatMuteResponse> toggleChatMute(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    ) {
        return ResponseEntity.ok(chatService.toggleMute(userId, chatRoomId));
    }
}
