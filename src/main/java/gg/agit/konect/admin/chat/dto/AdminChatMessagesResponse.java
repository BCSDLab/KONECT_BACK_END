package gg.agit.konect.admin.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminChatMessagesResponse(
    @Schema(description = "전체 메시지 수", example = "100")
    Long totalCount,

    @Schema(description = "현재 페이지 메시지 수", example = "20")
    Integer currentCount,

    @Schema(description = "전체 페이지 수", example = "5")
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1")
    Integer currentPage,

    @Schema(description = "메시지 목록")
    List<InnerAdminChatMessageResponse> messages
) {

    public static AdminChatMessagesResponse from(Page<ChatMessage> messages) {
        List<InnerAdminChatMessageResponse> responses = messages.getContent().stream()
            .map(InnerAdminChatMessageResponse::from)
            .toList();

        return new AdminChatMessagesResponse(
            messages.getTotalElements(),
            messages.getNumberOfElements(),
            messages.getTotalPages(),
            messages.getNumber() + 1,
            responses
        );
    }

    public record InnerAdminChatMessageResponse(
        @Schema(description = "메시지 ID", example = "1")
        Integer messageId,

        @Schema(description = "발신자 ID", example = "5")
        Integer senderId,

        @Schema(description = "발신자 이름", example = "홍길동")
        String senderName,

        @Schema(description = "발신자 프로필 이미지 URL")
        String senderProfileImage,

        @Schema(description = "발신자가 어드민인지 여부", example = "true")
        Boolean isAdmin,

        @Schema(description = "메시지 내용", example = "안녕하세요!")
        String content,

        @Schema(description = "메시지 생성 시간")
        LocalDateTime createdAt,

        @Schema(description = "읽음 여부", example = "true")
        Boolean isRead
    ) {

        public static InnerAdminChatMessageResponse from(ChatMessage chatMessage) {
            User sender = chatMessage.getSender();
            return new InnerAdminChatMessageResponse(
                chatMessage.getId(),
                sender.getId(),
                sender.getName(),
                sender.getImageUrl(),
                sender.getRole() == UserRole.ADMIN,
                chatMessage.getContent(),
                chatMessage.getCreatedAt(),
                chatMessage.getIsRead()
            );
        }
    }
}
