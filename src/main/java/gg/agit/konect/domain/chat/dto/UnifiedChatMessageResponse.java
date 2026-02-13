package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnifiedChatMessageResponse(
    @Schema(description = "메시지 ID", example = "505", requiredMode = REQUIRED)
    Integer messageId,

    @Schema(description = "발신자 ID", example = "1", requiredMode = REQUIRED)
    Integer senderId,

    @Schema(description = "발신자 이름(그룹채팅에서 제공)", example = "홍길동", requiredMode = NOT_REQUIRED)
    String senderName,

    @Schema(description = "메시지 내용", example = "안녕하세요!", requiredMode = REQUIRED)
    String content,

    @Schema(description = "메시지 전송 시간", example = "2025.07.23 15:53:12.123", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd HH:mm:ss.SSS")
    LocalDateTime createdAt,

    @Schema(description = "읽음 여부(1:1 채팅에서 제공)", example = "false", requiredMode = NOT_REQUIRED)
    Boolean isRead,

    @Schema(description = "미확인 인원 수(그룹채팅에서 제공)", example = "3", requiredMode = NOT_REQUIRED)
    Integer unreadCount,

    @Schema(description = "내가 보낸 메시지 여부", example = "true", requiredMode = REQUIRED)
    Boolean isMine
) {
}
