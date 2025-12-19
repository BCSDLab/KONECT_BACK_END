package gg.agit.konect.domain.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.chat.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Chat: 채팅", description = "채팅 API")
@RequestMapping("/chats")
public interface ChatApi {

    @Operation(summary = "문의하기 리스트를 조회한다.", description = """
        - 문의방이 존재하지만, 문의 이력이 없는 경우 : lastMessage, lastSentTime = null
        """)
    @GetMapping("/rooms")
    ResponseEntity<ChatRoomsResponse> getChatRooms(@UserId Integer userId);

    @GetMapping("/rooms/{chatRoomId}")
    ResponseEntity<ChatMessagesResponse> getChatRoomMessages(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    );
}
