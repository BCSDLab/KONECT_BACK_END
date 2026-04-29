package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record ChatRoomMembersInviteRequest(
    @NotEmpty(message = "초대할 유저 ID 목록은 필수입니다.")
    @Schema(description = "초대할 유저 ID 목록", example = "[10, 11, 12]", requiredMode = REQUIRED)
    List<Integer> userIds
) {
}
