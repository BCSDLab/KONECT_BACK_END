package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ChatRoomCreateRequest(
    @NotNull(message = "유저 ID는 필수입니다.")
    @Schema(description = "채팅 대상 유저 ID (1:1 채팅 시)", example = "10", requiredMode = REQUIRED)
    Integer userId
) {

    public record Group(
        @NotEmpty(message = "초대할 유저 ID 목록은 필수입니다.")
        @Schema(description = "초대할 유저 ID 목록", example = "[10, 11, 12]", requiredMode = REQUIRED)
        List<Integer> userIds
    ) {
    }
}
