package gg.agit.konect.domain.chat.unified.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.chat.direct.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.direct.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.direct.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatMuteResponse;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatMessageResponse;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatMessagesResponse;
import gg.agit.konect.domain.chat.unified.dto.UnifiedChatRoomsResponse;
import gg.agit.konect.domain.chat.unified.enums.ChatType;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Chat: 채팅", description = "채팅 API")
@RequestMapping("/chats")
public interface ChatApi {

    @Operation(summary = "채팅방을 생성하거나 기존 채팅방을 반환한다.", description = """
        ## 설명
        - 특정 유저와의 1:1 채팅방을 생성하거나 기존 채팅방을 반환합니다.
        
        ## 로직
        - 해당 유저와의 채팅방이 이미 존재하면 기존 채팅방을 반환합니다.
        - 존재하지 않으면 새로운 채팅방을 생성합니다.
        - 자기 자신과는 채팅방을 만들 수 없습니다.
        
        ## 에러
        - CANNOT_CREATE_CHAT_ROOM_WITH_SELF (400): 자기 자신과는 채팅방을 만들 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PostMapping("/rooms")
    ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @Valid @RequestBody ChatRoomCreateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "채팅방 리스트를 조회한다.", description = """
        ## 설명
        - 현재 사용자가 참여 중인 모든 채팅방 목록을 조회합니다.
        
        ## 로직
        - 각 채팅방의 상대방 정보, 마지막 메시지, 읽지 않은 메시지 수를 포함합니다.
        - 채팅방이 존재하지만 메시지 이력이 없는 경우 lastMessage, lastSentTime은 null입니다.
        - 최근 메시지가 있는 순서대로 정렬됩니다.
        """)
    @GetMapping("/rooms")
    ResponseEntity<UnifiedChatRoomsResponse> getChatRooms(
        @UserId Integer userId
    );

    @Operation(summary = "채팅방 메시지 리스트를 조회한다.", description = """
        ## 설명
        - 특정 채팅방의 메시지 목록을 페이지네이션으로 조회합니다.
        
        ## 로직
        - 채팅방에 진입하면 읽지 않은 메시지를 자동으로 읽음 처리합니다.
        - 최신 메시지가 먼저 오도록 정렬됩니다 (DESC).
        - isMine 필드로 내가 보낸 메시지인지 구분할 수 있습니다.
        - 채팅방 참여자만 메시지를 조회할 수 있습니다.
        - 일반 유저는 자신이 참여한 채팅방만 조회할 수 있습니다.
        - 어드민은 모든 어드민 채팅방을 조회할 수 있습니다.

        ## 에러
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        """)
    @GetMapping("/rooms/{chatRoomId}")
    ResponseEntity<UnifiedChatMessagesResponse> getChatRoomMessages(
        @RequestParam(name = "type") ChatType type,
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    );

    @Operation(summary = "메시지를 전송한다.", description = """
        ## 설명
        - 채팅방에 메시지를 전송합니다.
        
        ## 로직
        - 일반 유저는 자신이 참여한 채팅방에만 메시지를 전송할 수 있습니다.
        - 어드민은 모든 어드민 채팅방에 메시지를 전송할 수 있습니다.
        - 발신자는 자동으로 현재 사용자로 설정됩니다.
        - 수신자는 채팅방 상대방(어드민 채팅방의 경우 일반 유저)으로 자동 설정됩니다.
        - 전송된 메시지 정보를 응답으로 반환합니다.
        
        ## 에러
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        """)
    @PostMapping("/rooms/{chatRoomId}/messages")
    ResponseEntity<UnifiedChatMessageResponse> sendMessage(
        @RequestParam(name = "type") ChatType type,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "단체 채팅 알림 음소거를 토글한다.")
    @PostMapping("/groups/{clubId}/mute")
    ResponseEntity<GroupChatMuteResponse> toggleGroupChatMute(
        @PathVariable(value = "clubId") Integer clubId,
        @UserId Integer userId
    );
}
