package gg.agit.konect.domain.groupchat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.groupchat.dto.GroupChatMessageResponse;
import gg.agit.konect.domain.groupchat.dto.GroupChatMessagesResponse;
import gg.agit.konect.domain.groupchat.dto.GroupChatMuteResponse;
import gg.agit.konect.domain.groupchat.dto.GroupChatRoomResponse;
import gg.agit.konect.domain.groupchat.service.GroupChatService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clubs/{clubId}/group-chat")
public class GroupChatController implements GroupChatApi {

    private final GroupChatService groupChatService;

    public GroupChatController(GroupChatService groupChatService) {
        this.groupChatService = groupChatService;
    }

    @Override
    public ResponseEntity<GroupChatRoomResponse> getGroupChatRoom(
        @PathVariable Integer clubId,
        @UserId Integer userId
    ) {
        GroupChatRoomResponse response = groupChatService.getGroupChatRoom(clubId, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<GroupChatMessagesResponse> getMessages(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable Integer clubId,
        @UserId Integer userId
    ) {
        GroupChatMessagesResponse response = groupChatService.getMessages(clubId, userId, page, limit);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<GroupChatMessageResponse> sendMessage(
        @PathVariable Integer clubId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    ) {
        GroupChatMessageResponse response = groupChatService.sendMessage(clubId, userId, request.content());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> markAsRead(
        @PathVariable Integer clubId,
        @UserId Integer userId
    ) {
        groupChatService.markAsRead(clubId, userId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<GroupChatMuteResponse> toggleMute(
        @PathVariable Integer clubId,
        @UserId Integer userId
    ) {
        Boolean isMuted = groupChatService.toggleMute(clubId, userId);
        return ResponseEntity.ok(new GroupChatMuteResponse(isMuted));
    }
}
