package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;

import gg.agit.konect.domain.club.model.ClubApply;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubAppliedClubsResponse(
    @Schema(description = "가입 승인 대기 중인 동아리 리스트", requiredMode = REQUIRED)
    List<InnerAppliedClubResponse> appliedClubs
) {

    public record InnerAppliedClubResponse(
        @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "동아리 이름", example = "BCSD", requiredMode = REQUIRED)
        String name,

        @Schema(
            description = "동아리 이미지 링크",
            example = "https://bcsdlab.com/static/img/logo.d89d9cc.png",
            requiredMode = REQUIRED
        )
        String imageUrl,

        @Schema(description = "동아리 분과", example = "학술", requiredMode = REQUIRED)
        String categoryName,

        @Schema(description = "가입 신청 일시", example = "2025-01-13T10:30:00", requiredMode = REQUIRED)
        LocalDateTime appliedAt
    ) {
        public static InnerAppliedClubResponse from(ClubApply clubApply) {
            return new InnerAppliedClubResponse(
                clubApply.getClub().getId(),
                clubApply.getClub().getName(),
                clubApply.getClub().getImageUrl(),
                clubApply.getClub().getClubCategory().getDescription(),
                clubApply.getCreatedAt()
            );
        }
    }

    public static ClubAppliedClubsResponse from(List<ClubApply> clubApplies) {
        return new ClubAppliedClubsResponse(
            clubApplies.stream()
                .map(InnerAppliedClubResponse::from)
                .toList()
        );
    }
}
