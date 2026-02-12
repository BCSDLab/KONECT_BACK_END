package gg.agit.konect.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRoomCreateRequest(
    @Schema(description = "동아리 ID (동아리 회장과 채팅 시 사용)", example = "1")
    Integer clubId,

    @Schema(description = "대상 유저 ID (특정 유저와 직접 채팅 시 사용)", example = "10")
    Integer targetUserId
) {

    public boolean hasClubId() {
        return clubId != null;
    }

    public boolean hasTargetUserId() {
        return targetUserId != null;
    }
}
