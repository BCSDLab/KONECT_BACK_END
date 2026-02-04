package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.ClubMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMemberResponse(
    @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer clubId,

    @Schema(description = "회원 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer userId,

    @Schema(description = "회원 이름", example = "홍길동", requiredMode = REQUIRED)
    String userName,

    @Schema(description = "회원 직책", example = "MANAGER", requiredMode = REQUIRED)
    ClubPosition position
) {
    public static ClubMemberResponse from(ClubMember clubMember) {
        return new ClubMemberResponse(
            clubMember.getClub().getId(),
            clubMember.getUser().getId(),
            clubMember.getUser().getName(),
            clubMember.getClubPosition()
        );
    }
}
