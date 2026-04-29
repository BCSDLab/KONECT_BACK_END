package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_IN_NON_GROUP_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_ROOM_OWNER;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_KICK_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_INVITE_IN_NON_GROUP_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_LEAVE_GROUP_CHAT_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_KICK;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_USER;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomMemberCommandService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatRoomMembershipService chatRoomMembershipService;

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

    public void inviteMembers(Integer requesterId, Integer roomId, List<Integer> userIds) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        validateGroupRoomForInvite(room);
        validateActiveRequester(roomId, requesterId);

        List<Integer> distinctUserIds = userIds.stream()
            .distinct()
            .toList();
        List<User> requestedUsers = userRepository.findAllByIdIn(distinctUserIds);
        if (requestedUsers.size() != distinctUserIds.size()) {
            throw CustomException.of(NOT_FOUND_USER);
        }

        Set<Integer> existingActiveUserIds = Set.copyOf(
            chatRoomMemberRepository.findActiveUserIdsByChatRoomIdAndUserIdIn(roomId, distinctUserIds)
        );
        LocalDateTime joinedAt = LocalDateTime.now();
        requestedUsers.stream()
            .filter(user -> !user.getId().equals(requesterId))
            .filter(user -> !existingActiveUserIds.contains(user.getId()))
            .forEach(user -> chatRoomMembershipService.ensureMember(room, user, joinedAt));
    }

    private ChatRoomMember getRoomMember(Integer roomId, Integer userId) {
        return ChatRoomMemberLookup.getRoomMember(chatRoomMemberRepository, roomId, userId);
    }

    private void validateGroupRoomForKick(ChatRoom room) {
        if (!room.isGroupRoom() || room.isClubGroupRoom()) {
            throw CustomException.of(CANNOT_KICK_IN_NON_GROUP_ROOM);
        }
    }

    private void validateGroupRoomForInvite(ChatRoom room) {
        if (!room.isGroupRoom() || room.isClubGroupRoom()) {
            throw CustomException.of(CANNOT_INVITE_IN_NON_GROUP_ROOM);
        }
    }

    private void validateActiveRequester(Integer roomId, Integer requesterId) {
        if (!chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(roomId, requesterId)) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
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
