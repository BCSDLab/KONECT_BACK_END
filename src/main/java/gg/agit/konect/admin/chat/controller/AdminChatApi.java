package gg.agit.konect.admin.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.admin.chat.dto.AdminChatMessagesResponse;
import gg.agit.konect.admin.chat.dto.AdminChatMessagesResponse.InnerAdminChatMessageResponse;
import gg.agit.konect.admin.chat.dto.AdminChatRoomsResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.auth.annotation.Auth;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Admin) Chat: 채팅", description = "어드민 채팅 API")
@RequestMapping("/admin/chats")
@Auth(roles = {UserRole.ADMIN})
public interface AdminChatApi {

    @Operation(summary = "유저와의 채팅방을 생성하거나 기존 채팅방을 반환한다.", description = """
        특정 유저와의 1:1 채팅방을 생성하거나 기존 채팅방을 반환합니다.
        이미 해당 유저와 어드민 사이의 채팅방이 존재하면 기존 채팅방 ID를 반환합니다.
        """)
    @PostMapping("/rooms/users/{userId}")
    ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @PathVariable Integer userId,
        @UserId Integer adminId
    );

    @Operation(summary = "어드민 채팅방 목록을 조회한다.", description = """
        어드민과 대화한 모든 채팅방 목록을 조회합니다.
        송신자 또는 수신자 중 한 명이 어드민인 채팅방들이 조회됩니다.
        """)
    @GetMapping("/rooms")
    ResponseEntity<AdminChatRoomsResponse> getChatRooms();

    @Operation(summary = "어드민 채팅방의 메시지를 조회한다.", description = """
        채팅방의 메시지 목록을 조회하고, 어드민 대상 미읽음 메시지를 읽음 처리합니다.
        """)
    @GetMapping("/rooms/{chatRoomId}")
    ResponseEntity<AdminChatMessagesResponse> getChatRoomMessages(
        @PathVariable Integer chatRoomId,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "20") Integer limit,
        @UserId Integer adminId
    );

    @Operation(summary = "메시지를 전송한다.", description = """
        채팅방에 메시지를 전송합니다.
        발신자는 현재 로그인한 어드민, 수신자는 채팅방의 일반 사용자가 됩니다.
        """)
    @PostMapping("/rooms/{chatRoomId}/messages")
    ResponseEntity<InnerAdminChatMessageResponse> sendMessage(
        @PathVariable Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer adminId
    );
}
