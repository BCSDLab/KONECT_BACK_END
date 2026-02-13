package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.chat.enums.ChatRoomType;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRoomsResponse(
    @Schema(description = "채팅방 리스트", requiredMode = REQUIRED)
    List<InnerChatRoomResponse> chatRooms
) {
    public record InnerChatRoomResponse(
        @Schema(description = "채팅방 ID", example = "1", requiredMode = REQUIRED)
        Integer chatRoomId,

        @Schema(description = "상대방 이름", example = "김혜준", requiredMode = REQUIRED)
        String chatPartnerName,

        @Schema(description = "상대방 프로필 사진", example = "https://bcsdlab.com/static/img/logo.d89d9cc.png", requiredMode = REQUIRED)
        String chatPartnerProfileImage,

        @Schema(description = "마지막 메시지", example = "지원 어디서 해요", requiredMode = NOT_REQUIRED)
        String lastMessage,

        @Schema(description = "마지막 메시지 전송 시간", example = "2025.12.19 23:21", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
        LocalDateTime lastSentTime,

        @Schema(description = "읽지 않은 메시지 개수", example = "12", requiredMode = REQUIRED)
        Integer unreadCount,

        @Schema(description = "채팅방 타입", example = "NORMAL", requiredMode = REQUIRED)
        ChatRoomType chatRoomType
    ) {
        public static InnerChatRoomResponse from(
            ChatRoom chatRoom,
            User currentUser,
            Map<Integer, Integer> unreadCountMap,
            ChatRoomType chatRoomType
        ) {
            User chatPartner = chatRoom.getChatPartner(currentUser);

            return new InnerChatRoomResponse(
                chatRoom.getId(),
                chatPartner.getName(),
                chatPartner.getImageUrl(),
                chatRoom.getLastMessageContent(),
                chatRoom.getLastMessageSentAt(),
                unreadCountMap.getOrDefault(chatRoom.getId(), 0),
                chatRoomType
            );
        }

        public static InnerChatRoomResponse fromForAdmin(
            ChatRoom chatRoom,
            Map<Integer, Integer> unreadCountMap
        ) {
            User nonAdminUser = chatRoom.getNonAdminUser();

            return new InnerChatRoomResponse(
                chatRoom.getId(),
                nonAdminUser.getName(),
                nonAdminUser.getImageUrl(),
                chatRoom.getLastMessageContent(),
                chatRoom.getLastMessageSentAt(),
                unreadCountMap.getOrDefault(chatRoom.getId(), 0),
                ChatRoomType.ADMIN
            );
        }
    }

    public static ChatRoomsResponse of(List<InnerChatRoomResponse> chatRooms) {
        return new ChatRoomsResponse(chatRooms);
    }
}
