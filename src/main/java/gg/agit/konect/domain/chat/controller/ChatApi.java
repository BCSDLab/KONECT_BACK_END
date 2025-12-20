package gg.agit.konect.domain.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.chat.dto.ChatMessageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.domain.chat.dto.CreateChatRoomRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Chat: 채팅", description = "채팅 API")
@RequestMapping("/chats")
public interface ChatApi {

    @Operation(summary = "채팅방을 생성하거나 기존 채팅방을 반환한다.", description = """
        - 해당 동아리 회장과의 채팅방이 이미 존재하면 기존 채팅방을 반환합니다.
        - 존재하지 않으면 새로 생성합니다.
        """)
    @PostMapping("/rooms")
    ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @Valid @RequestBody CreateChatRoomRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "문의하기 리스트를 조회한다.", description = """
        - 문의방이 존재하지만, 문의 이력이 없는 경우 : lastMessage, lastSentTime = null
        """)
    @GetMapping("/rooms")
    ResponseEntity<ChatRoomsResponse> getChatRooms(@UserId Integer userId);

    @Operation(summary = "문의하기 메시지 리스트를 조회한다.")
    @GetMapping("/rooms/{chatRoomId}")
    ResponseEntity<ChatMessagesResponse> getChatRoomMessages(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    );

    @Operation(summary = "메시지를 전송한다.")
    @PostMapping("/rooms/{chatRoomId}/messages")
    ResponseEntity<ChatMessageResponse> sendMessage(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    );
}
