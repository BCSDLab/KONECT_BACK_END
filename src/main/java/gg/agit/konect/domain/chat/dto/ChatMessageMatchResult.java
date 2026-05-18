package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatMessageMatchResult(
    @Schema(description = "채팅방 ID", example = "1", requiredMode = REQUIRED)
    Integer roomId,

    @Schema(description = "채팅 타입", example = "DIRECT", requiredMode = REQUIRED)
    ChatType chatType,

    @Schema(description = "채팅방 이름", example = "개발팀", requiredMode = REQUIRED)
    String roomName,

    @Schema(description = "채팅방 이미지 URL", example = "https://example.com/image.png", requiredMode = NOT_REQUIRED)
    String roomImageUrl,

    @Schema(description = "검색에 매칭된 메시지 내용", example = "안녕하세요", requiredMode = REQUIRED)
    String matchedMessage,

    @Schema(description = "매칭된 메시지 전송 시간", example = "2025.12.19 23:21", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
    LocalDateTime matchedMessageSentAt,

    @Schema(description = "검색에 매칭된 메시지 ID", example = "42", requiredMode = REQUIRED)
    Integer matchedMessageId,

    @Schema(description = "읽지 않은 메시지 수", example = "3", requiredMode = REQUIRED)
    Integer unreadCount,

    @Schema(description = "채팅방 알림 뮤트 여부", example = "false", requiredMode = REQUIRED)
    Boolean isMuted
) {

    public static ChatMessageMatchResult from(ChatRoomSummaryResponse room, ChatMessage message) {
        return new ChatMessageMatchResult(
            room.roomId(),
            room.chatType(),
            room.roomName(),
            room.roomImageUrl(),
            message.getContent(),
            message.getCreatedAt(),
            message.getId(),
            room.unreadCount(),
            room.isMuted()
        );
    }
}
