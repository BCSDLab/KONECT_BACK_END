package gg.agit.konect.domain.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Chat: 채팅", description = "채팅 API")
@RequestMapping("/chats")
public interface ChatApi {

    @GetMapping("/rooms")
    ResponseEntity<ChatRoomsResponse> getChatRooms(@UserId Integer userId);
}
