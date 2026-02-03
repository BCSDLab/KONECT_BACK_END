package gg.agit.konect.admin.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminChatRoomsResponse(
    @Schema(description = "채팅방 목록")
    List<InnerAdminChatRoomResponse> chatRooms
) {

    public record InnerAdminChatRoomResponse(
        @Schema(description = "채팅방 ID", example = "1")
        Integer chatRoomId,

        @Schema(description = "일반 사용자 이름", example = "홍길동")
        String userName,

        @Schema(description = "일반 사용자 프로필 이미지 URL")
        String userProfileImage,

        @Schema(description = "마지막 메시지 내용", example = "안녕하세요!")
        String lastMessage,

        @Schema(description = "마지막 메시지 전송 시간")
        LocalDateTime lastSentTime,

        @Schema(description = "미읽음 메시지 수", example = "3")
        Integer unreadCount
    ) {

        public static InnerAdminChatRoomResponse from(
            ChatRoom chatRoom,
            User normalUser,
            Integer unreadCount
        ) {
            return new InnerAdminChatRoomResponse(
                chatRoom.getId(),
                normalUser.getName(),
                normalUser.getImageUrl(),
                chatRoom.getLastMessageContent(),
                chatRoom.getLastMessageSentAt(),
                unreadCount
            );
        }
    }
}
