package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.chat.model.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatMessagesResponse(
    @Schema(description = "채팅 메시지 리스트", requiredMode = REQUIRED)
    List<InnerChatMessageResponse> messages
) {
    public record InnerChatMessageResponse(
        @Schema(description = "메시지 ID", example = "505", requiredMode = REQUIRED)
        Integer messageId,

        @Schema(description = "발신자 ID", example = "1", requiredMode = REQUIRED)
        Integer senderId,

        @Schema(description = "메시지 내용", example = "투명 케이스가 끼워져 있었어요!", requiredMode = REQUIRED)
        String content,

        @Schema(description = "메시지 전송 시간", example = "2025.07.23 15:53", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
        LocalDateTime createdAt,

        @Schema(description = "읽음 여부", example = "false", requiredMode = REQUIRED)
        Boolean isRead,

        @Schema(description = "내가 보낸 메시지 여부", example = "true", requiredMode = REQUIRED)
        Boolean isMine
    ) {
        public static InnerChatMessageResponse from(ChatMessage message, Integer currentUserId) {
            return new InnerChatMessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getCreatedAt(),
                message.getIsRead(),
                message.isSentBy(currentUserId)
            );
        }
    }

    public static ChatMessagesResponse from(List<ChatMessage> messages, Integer currentUserId) {
        return new ChatMessagesResponse(
            messages.stream()
                .map(message -> InnerChatMessageResponse.from(message, currentUserId))
                .toList()
        );
    }
}
