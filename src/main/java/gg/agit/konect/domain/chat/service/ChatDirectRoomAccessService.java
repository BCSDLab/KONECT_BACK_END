package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatDirectRoomAccessService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public ChatRoomMember getAccessibleMember(ChatRoom chatRoom, User user) {
        ChatRoomMember member = getMember(chatRoom, user);
        restoreIfVisible(member, chatRoom);
        return member;
    }

    public LocalDateTime prepareAccessAndGetVisibleMessageFrom(ChatRoom chatRoom, User user) {
        ChatRoomMember member = getMember(chatRoom, user);
        LocalDateTime visibleMessageFrom = member.getVisibleMessageFrom();
        restoreIfVisible(member, chatRoom);
        return visibleMessageFrom;
    }

    private ChatRoomMember getMember(ChatRoom chatRoom, User user) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
            .orElseThrow(() -> CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS));
    }

    /**
     * direct 채팅방에서 나간 사용자가 다시 볼 수 있는 상태인지 확인하고,
     * 새 메시지가 이미 존재하면 나간 상태를 해제한다.
     */
    private void restoreIfVisible(ChatRoomMember member, ChatRoom chatRoom) {
        if (!member.hasLeft()) {
            return;
        }

        if (!member.hasVisibleMessages(chatRoom)) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        member.restoreDirectRoom();
    }
}
