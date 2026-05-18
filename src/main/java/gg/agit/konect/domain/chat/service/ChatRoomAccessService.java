package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomAccessService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatRoomMembershipService chatRoomMembershipService;
    private final ChatRoomSystemAdminService chatRoomSystemAdminService;
    private final ChatDirectRoomAccessService chatDirectRoomAccessService;

    public ChatRoomMember getAccessibleMember(ChatRoom room, Integer userId) {
        if (room.isDirectRoom()) {
            User user = userRepository.getById(userId);
            return chatDirectRoomAccessService.getAccessibleMember(room, user);
        }

        return getAccessibleNonDirectMember(room, userId);
    }

    public ChatRoomMember getAccessibleMember(ChatRoom room, User user) {
        if (room.isDirectRoom()) {
            return chatDirectRoomAccessService.getAccessibleMember(room, user);
        }

        return getAccessibleNonDirectMember(room, user.getId());
    }

    private ChatRoomMember getAccessibleNonDirectMember(ChatRoom room, Integer userId) {
        if (room.isClubGroupRoom()) {
            chatRoomMembershipService.ensureClubRoomMember(room, userId);
            return getRoomMember(room.getId(), userId);
        }

        ChatRoomMember member = getRoomMember(room.getId(), userId);
        if (member.hasLeft()) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }
        return member;
    }

    public void ensureMuteAccess(ChatRoom room, User user) {
        if (room.isDirectRoom() && user.isAdmin() && chatRoomSystemAdminService.isSystemAdminRoom(room.getId())) {
            return;
        }

        getAccessibleMember(room, user);
    }

    private ChatRoomMember getRoomMember(Integer roomId, Integer userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
            .orElseThrow(() -> CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS));
    }
}
