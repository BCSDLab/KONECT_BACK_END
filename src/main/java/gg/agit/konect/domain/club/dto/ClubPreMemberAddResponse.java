package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.model.ClubPreMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubPreMemberAddResponse(
    @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer clubId,

    @Schema(description = "학번", example = "2021136089", requiredMode = REQUIRED)
    String studentNumber,

    @Schema(description = "이름", example = "홍길동", requiredMode = REQUIRED)
    String name
) {
    public static ClubPreMemberAddResponse from(ClubPreMember preMember) {
        return new ClubPreMemberAddResponse(
            preMember.getClub().getId(),
            preMember.getStudentNumber(),
            preMember.getName()
        );
    }
}
