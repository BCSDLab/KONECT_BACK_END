package gg.agit.konect.domain.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
