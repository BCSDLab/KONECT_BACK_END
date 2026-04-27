package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_MEMBER;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessagePageResolver {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ClubMemberRepository clubMemberRepository;

    public int resolvePageForMessage(
        Integer roomId,
        Integer messageId,
        ChatRoom room,
        User user,
        int limit
    ) {
        ensureMessageLookupAccess(room, user);

        ChatMessage targetMessage = chatMessageRepository.findByIdWithChatRoom(messageId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        if (!targetMessage.getChatRoom().getId().equals(roomId)) {
            throw CustomException.of(NOT_FOUND_CHAT_ROOM);
        }

        LocalDateTime visibleMessageFrom = resolveVisibleMessageFrom(room, user);
        if (visibleMessageFrom != null && !targetMessage.getCreatedAt().isAfter(visibleMessageFrom)) {
            throw CustomException.of(NOT_FOUND_CHAT_ROOM);
        }

        long newerCount = chatMessageRepository.countNewerMessagesByChatRoomId(
            roomId, messageId, targetMessage.getCreatedAt(), visibleMessageFrom
        );
        return (int)(newerCount / limit) + 1;
    }

    /**
     * messageId 조회 전 방 접근 권한을 검증한다.
     * 권한 없음과 메시지 미존재를 구분할 수 없게 NOT_FOUND_CHAT_ROOM으로 통일한다.
     */
    private void ensureMessageLookupAccess(ChatRoom room, User user) {
        if (room.isDirectRoom()) {
            boolean isMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(room.getId(), user.getId())
                .isPresent();
            if (!isMember && !(user.isAdmin() && isSystemAdminRoom(room))) {
                throw CustomException.of(NOT_FOUND_CHAT_ROOM);
            }
            return;
        }

        if (room.isClubGroupRoom()) {
            try {
                clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), user.getId());
            } catch (CustomException e) {
                if (e.getErrorCode() == NOT_FOUND_CLUB_MEMBER) {
                    throw CustomException.of(NOT_FOUND_CHAT_ROOM);
                }
                throw e;
            }
            return;
        }

        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .filter(member -> !member.hasLeft())
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
    }

    private LocalDateTime resolveVisibleMessageFrom(ChatRoom room, User user) {
        if (!room.isDirectRoom()) {
            return null;
        }

        if (user.isAdmin() && isSystemAdminRoom(room)) {
            List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(room.getId());
            ChatRoomMember systemAdminMember = findRoomMember(members, SYSTEM_ADMIN_ID);
            return systemAdminMember != null ? systemAdminMember.getVisibleMessageFrom() : null;
        }

        return chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .map(ChatRoomMember::getVisibleMessageFrom)
            .orElse(null);
    }

    private boolean isSystemAdminRoom(ChatRoom chatRoom) {
        List<Object[]> memberIds = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(
            List.of(chatRoom.getId())
        );
        return memberIds.stream()
            .map(row -> (Integer)row[1])
            .anyMatch(userId -> userId.equals(SYSTEM_ADMIN_ID));
    }

    private ChatRoomMember findRoomMember(List<ChatRoomMember> members, Integer userId) {
        return members.stream()
            .filter(member -> member.getUserId().equals(userId))
            .findFirst()
            .orElse(null);
    }
}
