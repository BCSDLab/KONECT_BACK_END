package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.model.ClubPosition;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubPositionsResponse(
    @Schema(description = "직책 리스트", requiredMode = REQUIRED)
    List<InnerClubPosition> positions
) {
    public record InnerClubPosition(
        @Schema(description = "직책 ID", example = "1", requiredMode = REQUIRED)
        Integer positionId,

        @Schema(description = "직책 이름", example = "회장", requiredMode = REQUIRED)
        String name,

        @Schema(description = "직책 그룹", example = "PRESIDENT", requiredMode = REQUIRED)
        ClubPositionGroup positionGroup,

        @Schema(description = "우선순위 (낮을수록 높은 직급)", example = "0", requiredMode = REQUIRED)
        Integer priority,

        @Schema(description = "해당 직책의 회원 수", example = "1", requiredMode = REQUIRED)
        Long memberCount,

        @Schema(description = "이름 변경 가능 여부", example = "false", requiredMode = REQUIRED)
        Boolean canRename,

        @Schema(description = "삭제 가능 여부", example = "false", requiredMode = REQUIRED)
        Boolean canDelete
    ) {
        public static InnerClubPosition of(
            ClubPosition position,
            Long memberCount
        ) {
            return new InnerClubPosition(
                position.getId(),
                position.getName(),
                position.getClubPositionGroup(),
                position.getPriority(),
                memberCount,
                position.canRename(),
                position.canDelete()
            );
        }
    }

    public static ClubPositionsResponse of(List<InnerClubPosition> positions) {
        return new ClubPositionsResponse(positions);
    }
}
