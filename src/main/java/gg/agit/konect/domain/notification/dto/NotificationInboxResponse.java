package gg.agit.konect.domain.notification.dto;

import java.time.LocalDateTime;

import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.notification.model.NotificationInbox;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationInboxResponse(
    @Schema(description = "알림 ID", example = "1")
    Integer id,

    @Schema(description = "알림 타입", example = "CLUB_APPLICATION_SUBMITTED")
    NotificationInboxType type,

    @Schema(description = "알림 제목", example = "동아리 가입 신청이 접수되었습니다")
    String title,

    @Schema(description = "알림 내용", example = "신청하신 동아리의 가입 신청이 접수되었습니다")
    String body,

    @Schema(description = "알림 클릭 시 이동할 경로", example = "/clubs/1")
    String path,

    @Schema(description = "읽음 여부", example = "false")
    Boolean isRead,

    @Schema(description = "알림 생성 시간", example = "2024-01-15T10:30:00")
    LocalDateTime createdAt
) {
    public static NotificationInboxResponse from(NotificationInbox inbox) {
        return new NotificationInboxResponse(
            inbox.getId(),
            inbox.getType(),
            inbox.getTitle(),
            inbox.getBody(),
            inbox.getPath(),
            inbox.getIsRead(),
            inbox.getCreatedAt()
        );
    }
}
