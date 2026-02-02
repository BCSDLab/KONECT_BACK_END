package gg.agit.konect.admin.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.admin.chat.dto.AdminChatMessagesResponse;
import gg.agit.konect.admin.chat.dto.AdminChatMessagesResponse.InnerAdminChatMessageResponse;
import gg.agit.konect.admin.chat.dto.AdminChatRoomCreateRequest;
import gg.agit.konect.admin.chat.dto.AdminChatRoomsResponse;
import gg.agit.konect.admin.chat.service.AdminChatService;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/chats")
public class AdminChatController implements AdminChatApi {

    private final AdminChatService adminChatService;

    @Override
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        AdminChatRoomCreateRequest request,
        Integer adminId
    ) {
        ChatRoomResponse response = adminChatService.createOrGetChatRoom(request, adminId);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AdminChatRoomsResponse> getChatRooms(Integer adminId) {
        AdminChatRoomsResponse response = adminChatService.getChatRooms();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AdminChatMessagesResponse> getChatRoomMessages(
        Integer chatRoomId,
        Integer page,
        Integer limit,
        Integer adminId
    ) {
        AdminChatMessagesResponse response = adminChatService.getChatRoomMessages(
            chatRoomId, page, limit
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<InnerAdminChatMessageResponse> sendMessage(
        Integer chatRoomId,
        ChatMessageSendRequest request,
        Integer adminId
    ) {
        InnerAdminChatMessageResponse response = adminChatService.sendMessage(
            chatRoomId, request, adminId
        );

        return ResponseEntity.ok(response);
    }
}
