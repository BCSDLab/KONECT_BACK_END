package gg.agit.konect.domain.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.chat.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.domain.chat.service.ChatRoomService;
import gg.agit.konect.global.auth.annotation.UserId;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final ChatRoomService chatRoomService;

    @GetMapping("/rooms")
    public ResponseEntity<ChatRoomsResponse> getChatRooms(@UserId Integer userId) {
        ChatRoomsResponse response = chatRoomService.getChatRooms(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{chatRoomId}")
    public ResponseEntity<ChatMessagesResponse> getChatRoomMessages(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    ) {
        ChatMessagesResponse response = chatRoomService.getChatRoomMessages(userId, chatRoomId, page, limit);
        return ResponseEntity.ok(response);
    }
}
