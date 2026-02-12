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
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Chat: 채팅", description = "채팅 API")
@RequestMapping("/chats")
public interface ChatApi {

    @Operation(summary = "채팅방을 생성하거나 기존 채팅방을 반환한다.", description = """
        ## 설명
        - 1:1 채팅방을 생성하거나 기존 채팅방을 반환합니다.
        - clubId 또는 targetUserId 중 하나를 필수로 전달해야 합니다.

        ## 로직
        ### clubId를 전달한 경우
        - 해당 동아리 회장과의 채팅방을 생성하거나 기존 채팅방을 반환합니다.
        - 현재 사용자가 해당 동아리의 회장인 경우 자기 자신과 채팅방을 만들 수 없습니다.

        ### targetUserId를 전달한 경우
        - 해당 유저와의 채팅방을 생성하거나 기존 채팅방을 반환합니다.
        - 자기 자신과는 채팅방을 만들 수 없습니다.

        ## 에러
        - INVALID_CHAT_ROOM_CREATE_REQUEST (400): clubId 또는 targetUserId 중 하나만 전달해야 합니다.
        - NOT_FOUND_CLUB_PRESIDENT (404): 동아리 회장을 찾을 수 없습니다.
        - CANNOT_CREATE_CHAT_ROOM_WITH_SELF (400): 자기 자신과는 채팅방을 만들 수 없습니다.
        """)
    @PostMapping("/rooms")
    ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @Valid @RequestBody ChatRoomCreateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "문의하기 리스트를 조회한다.", description = """
        ## 설명
        - 현재 사용자가 참여 중인 모든 채팅방 목록을 조회합니다.

        ## 로직
        - 각 채팅방의 상대방 정보, 마지막 메시지, 읽지 않은 메시지 수를 포함합니다.
        - 채팅방이 존재하지만 메시지 이력이 없는 경우 lastMessage, lastSentTime은 null입니다.
        - 최근 메시지가 있는 순서대로 정렬됩니다.
        """)
    @GetMapping("/rooms")
    ResponseEntity<ChatRoomsResponse> getChatRooms(
        @UserId Integer userId
    );

    @Operation(summary = "문의하기 메시지 리스트를 조회한다.", description = """
        ## 설명
        - 특정 채팅방의 메시지 목록을 페이지네이션으로 조회합니다.

        ## 로직
        - 채팅방에 진입하면 읽지 않은 메시지를 자동으로 읽음 처리합니다.
        - 최신 메시지가 먼저 오도록 정렬됩니다 (DESC).
        - isMine 필드로 내가 보낸 메시지인지 구분할 수 있습니다.
        - 채팅방 참여자만 메시지를 조회할 수 있습니다.

        ## 에러
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        """)
    @GetMapping("/rooms/{chatRoomId}")
    ResponseEntity<ChatMessagesResponse> getChatRoomMessages(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    );

    @Operation(summary = "메시지를 전송한다.", description = """
        ## 설명
        - 채팅방에 메시지를 전송합니다.

        ## 로직
        - 채팅방 참여자만 메시지를 전송할 수 있습니다.
        - 발신자는 자동으로 현재 사용자로 설정됩니다.
        - 수신자는 채팅방 상대방으로 자동 설정됩니다.
        - 전송된 메시지 정보를 응답으로 반환합니다.

        ## 에러
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        """)
    @PostMapping("/rooms/{chatRoomId}/messages")
    ResponseEntity<ChatMessageResponse> sendMessage(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    );

}
