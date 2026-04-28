package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;

import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.global.exception.CustomException;

final class ChatRoomMemberLookup {

    private ChatRoomMemberLookup() {
    }

    static ChatRoomMember getByChatRoomIdAndUserId(
        ChatRoomMemberRepository chatRoomMemberRepository,
        Integer roomId,
        Integer userId
    ) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
            .orElseThrow(() -> CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS));
    }
}
