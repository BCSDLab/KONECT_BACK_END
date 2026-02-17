package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.user.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatMessagesResponse(
    @Schema(description = "조건에 해당하는 메시지 수", example = "100", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 메시지 수", example = "20", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "5", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

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

        @Schema(description = "메시지 전송 시간", example = "2025.07.23 15:53:12.123", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm:ss.SSS")
        LocalDateTime createdAt,

        @Schema(description = "읽음 여부", example = "false", requiredMode = REQUIRED)
        Boolean isRead,

        @Schema(description = "내가 보낸 메시지 여부", example = "true", requiredMode = REQUIRED)
        Boolean isMine
    ) {
        public static InnerChatMessageResponse from(
            ChatMessage message,
            Integer currentUserId,
            Integer maskedAdminId,
            LocalDateTime lastReadAt
        ) {
            Integer senderId = resolveSenderId(message, maskedAdminId);
            boolean isMine = message.isSentBy(currentUserId);
            boolean isRead = isMine || lastReadAt != null && !message.getCreatedAt().isAfter(lastReadAt);
            return new InnerChatMessageResponse(
                message.getId(),
                senderId,
                message.getContent(),
                message.getCreatedAt(),
                isRead,
                isMine
            );
        }

        private static Integer resolveSenderId(ChatMessage message, Integer maskedAdminId) {
            if (maskedAdminId != null && message.getSender().getRole() == UserRole.ADMIN) {
                return maskedAdminId;
            }
            return message.getSender().getId();
        }
    }

    public static ChatMessagesResponse from(
        Page<ChatMessage> page,
        Integer currentUserId,
        Integer maskedAdminId,
        LocalDateTime lastReadAt
    ) {
        return new ChatMessagesResponse(
            page.getTotalElements(),
            page.getNumberOfElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            page.stream()
                .map(message -> {
                    Integer senderId = InnerChatMessageResponse.resolveSenderId(message, maskedAdminId);
                    boolean isMine = message.isSentBy(currentUserId);
                    boolean isRead = isMine || lastReadAt != null && !message.getCreatedAt().isAfter(lastReadAt);
                    return new InnerChatMessageResponse(
                        message.getId(),
                        senderId,
                        message.getContent(),
                        message.getCreatedAt(),
                        isRead,
                        isMine
                    );
                })
                .toList()
        );
    }
}
