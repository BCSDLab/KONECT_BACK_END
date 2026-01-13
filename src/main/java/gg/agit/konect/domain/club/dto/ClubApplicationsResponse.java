package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;

import gg.agit.konect.domain.club.model.ClubApply;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubApplicationsResponse(
    @Schema(description = "동아리 지원 내역 리스트", requiredMode = REQUIRED)
    List<ClubApplicationResponse> applications
) {

    public record ClubApplicationResponse(
        @Schema(description = "지원 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "지원자 학번", example = "20250120", requiredMode = REQUIRED)
        String studentNumber,

        @Schema(description = "지원자 이름", example = "이동훈", requiredMode = REQUIRED)
        String name,

        @Schema(description = "지원 일시", example = "2025-01-13T10:30:00", requiredMode = REQUIRED)
        LocalDateTime appliedAt
    ) {

        public static ClubApplicationResponse from(ClubApply clubApply) {
            return new ClubApplicationResponse(
                clubApply.getId(),
                clubApply.getUser().getStudentNumber(),
                clubApply.getUser().getName(),
                clubApply.getCreatedAt()
            );
        }
    }

    public static ClubApplicationsResponse from(List<ClubApply> clubApplies) {
        return new ClubApplicationsResponse(
            clubApplies.stream()
                .map(ClubApplicationResponse::from)
                .toList()
        );
    }
}
