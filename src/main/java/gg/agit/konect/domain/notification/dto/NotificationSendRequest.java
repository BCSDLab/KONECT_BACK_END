package gg.agit.konect.domain.notification.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record NotificationSendRequest(
    @NotEmpty(message = "알림 제목은 필수 입력입니다.")
    @Schema(description = "알림 제목", example = "새로운 알림이 도착했어요", requiredMode = REQUIRED)
    String title,

    @NotEmpty(message = "알림 본문은 필수 입력입니다.")
    @Schema(description = "알림 본문", example = "확인해 주세요.", requiredMode = REQUIRED)
    String body,

    @Schema(description = "추가 데이터", example = "{\"type\":\"notice\"}")
    Map<String, String> data,

    @Schema(description = "딥링크 경로(https://agit.gg/ 이후)", example = "chats")
    String path
) {
}
