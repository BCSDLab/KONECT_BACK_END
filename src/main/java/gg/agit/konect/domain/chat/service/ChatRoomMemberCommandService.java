package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_IN_NON_GROUP_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_ROOM_OWNER;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_LEAVE_GROUP_CHAT_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_KICK;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomMemberCommandService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void leaveChatRoom(Integer userId, Integer roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (room.isClubGroupRoom()) {
            throw CustomException.of(CANNOT_LEAVE_GROUP_CHAT_ROOM);
        }

        ChatRoomMember member = getRoomMember(roomId, userId);
        if (room.isDirectRoom()) {
            member.leaveDirectRoom(LocalDateTime.now());
            return;
        }

        chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, userId);
    }

    public void kickMember(Integer requesterId, Integer roomId, Integer targetUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        validateGroupRoomForKick(room);
        validateNotSelfKick(requesterId, targetUserId);

        ChatRoomMember requester = getRoomMember(roomId, requesterId);
        validateKickAuthority(requester);

        ChatRoomMember target = getRoomMember(roomId, targetUserId);
        validateNotOwnerTarget(target);

        chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, targetUserId);
    }

    private ChatRoomMember getRoomMember(Integer roomId, Integer userId) {
        return ChatRoomMemberLookup.getRoomMember(chatRoomMemberRepository, roomId, userId);
    }

    private void validateGroupRoomForKick(ChatRoom room) {
        if (!room.isGroupRoom() || room.isClubGroupRoom()) {
            throw CustomException.of(CANNOT_KICK_IN_NON_GROUP_ROOM);
        }
    }

    private void validateNotSelfKick(Integer requesterId, Integer targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw CustomException.of(CANNOT_KICK_SELF);
        }
    }

    private void validateKickAuthority(ChatRoomMember requester) {
        if (!requester.isOwner()) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_KICK);
        }
    }

    private void validateNotOwnerTarget(ChatRoomMember target) {
        if (target.isOwner()) {
            throw CustomException.of(CANNOT_KICK_ROOM_OWNER);
        }
    }
}
