package gg.agit.konect.domain.notification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import gg.agit.konect.domain.notification.model.NotificationInbox;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationInboxesResponse(
    @Schema(description = "알림 목록")
    List<NotificationInboxResponse> notifications,

    @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
    int currentPage,

    @Schema(description = "총 페이지 수", example = "10")
    int totalPages,

    @Schema(description = "총 알림 개수", example = "100")
    long totalElements,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {
    public static NotificationInboxesResponse from(Page<NotificationInbox> page) {
        return new NotificationInboxesResponse(
            page.getContent().stream().map(NotificationInboxResponse::from).toList(),
            page.getNumber() + 1,
            page.getTotalPages(),
            page.getTotalElements(),
            page.hasNext()
        );
    }
}
