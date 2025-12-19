package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

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

        @Schema(description = "마지막 메시지", example = "지원 어디서 해요", requiredMode = REQUIRED)
        String lastMessage,

        @Schema(description = "마지막 메시지 전송 시간", example = "2025.12.19 23:21", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
        String lastSentTime,

        @Schema(description = "읽지 않은 메시지 개수", example = "12", requiredMode = REQUIRED)
        Integer unreadCount
    ) {

    }
}
