package gg.agit.konect.domain.chat.unified.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.chat.unified.enums.ChatType;
import io.swagger.v3.oas.annotations.media.Schema;

public record UnifiedChatRoomResponse(
    @Schema(description = "채팅방 ID", example = "1", requiredMode = REQUIRED)
    Integer roomId,

    @Schema(description = "채팅 타입", example = "DIRECT", requiredMode = REQUIRED)
    ChatType chatType,

    @Schema(description = "채팅방 이름", example = "김혜준", requiredMode = REQUIRED)
    String roomName,

    @Schema(description = "채팅방 이미지 URL", example = "https://bcsdlab.com/static/img/logo.d89d9cc.png", requiredMode = NOT_REQUIRED)
    String roomImageUrl,

    @Schema(description = "마지막 메시지", example = "안녕하세요!", requiredMode = NOT_REQUIRED)
    String lastMessage,

    @Schema(description = "마지막 메시지 전송 시간", example = "2025.12.19 23:21", requiredMode = NOT_REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
    LocalDateTime lastSentAt,

    @Schema(description = "읽지 않은 메시지 수", example = "12", requiredMode = REQUIRED)
    Integer unreadCount,

    @Schema(description = "채팅방 알림 뮤트 여부", example = "false", requiredMode = REQUIRED)
    Boolean isMuted
) {
}
