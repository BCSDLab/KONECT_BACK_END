package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record MyManagedClubResponse(
    @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer clubId,

    @Schema(description = "동아리 이름", example = "BCSD", requiredMode = REQUIRED)
    String clubName,

    @Schema(description = "회원 이름", example = "배진호", requiredMode = REQUIRED)
    String name,

    @Schema(description = "회원 학번", example = "2020136061", requiredMode = REQUIRED)
    String studentNumber,

    @Schema(description = "직책", example = "회장", requiredMode = REQUIRED)
    String position
) {
    public static MyManagedClubResponse from(Club club, ClubMember clubMember) {
        User user = clubMember.getUser();
        return new MyManagedClubResponse(
            club.getId(),
            club.getName(),
            user.getName(),
            user.getStudentNumber(),
            clubMember.getClubPosition().getName()
        );
    }
}
