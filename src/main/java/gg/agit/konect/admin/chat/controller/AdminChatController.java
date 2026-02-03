package gg.agit.konect.admin.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.admin.chat.dto.AdminChatMessagesResponse;
import gg.agit.konect.admin.chat.dto.AdminChatMessagesResponse.InnerAdminChatMessageResponse;
import gg.agit.konect.admin.chat.dto.AdminChatRoomsResponse;
import gg.agit.konect.admin.chat.service.AdminChatService;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/chats")
public class AdminChatController implements AdminChatApi {

    private final AdminChatService adminChatService;

    @Override
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @PathVariable Integer userId,
        @UserId Integer adminId
    ) {
        ChatRoomResponse response = adminChatService.createOrGetChatRoom(userId, adminId);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AdminChatRoomsResponse> getChatRooms() {
        AdminChatRoomsResponse response = adminChatService.getChatRooms();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AdminChatMessagesResponse> getChatRoomMessages(
        @PathVariable Integer chatRoomId,
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20") Integer limit,
        @UserId Integer adminId
    ) {
        AdminChatMessagesResponse response = adminChatService.getChatRoomMessages(
            chatRoomId, page, limit
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<InnerAdminChatMessageResponse> sendMessage(
        @PathVariable Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer adminId
    ) {
        InnerAdminChatMessageResponse response = adminChatService.sendMessage(
            chatRoomId, request, adminId
        );

        return ResponseEntity.ok(response);
    }
}
