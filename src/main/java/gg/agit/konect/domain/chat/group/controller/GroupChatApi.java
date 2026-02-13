package gg.agit.konect.domain.chat.group.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.chat.direct.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.group.dto.GroupChatMessageResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatMessagesResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatMuteResponse;
import gg.agit.konect.domain.chat.group.dto.GroupChatRoomResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) GroupChat: 단체 채팅", description = "단체 채팅 API")
@RequestMapping("/api/clubs/{clubId}/group-chat")
public interface GroupChatApi {

    @Operation(summary = "단체 채팅방 정보를 조회한다.")
    @GetMapping("/room")
    ResponseEntity<GroupChatRoomResponse> getGroupChatRoom(
        @PathVariable Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "단체 채팅 메시지 리스트를 조회한다.")
    @GetMapping("/messages")
    ResponseEntity<GroupChatMessagesResponse> getMessages(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "단체 채팅 메시지를 전송한다.")
    @PostMapping("/messages")
    ResponseEntity<GroupChatMessageResponse> sendMessage(
        @PathVariable Integer clubId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "단체 채팅 알림 음소거를 토글한다.")
    @PostMapping("/mute")
    ResponseEntity<GroupChatMuteResponse> toggleMute(
        @PathVariable Integer clubId,
        @UserId Integer userId
    );
}
