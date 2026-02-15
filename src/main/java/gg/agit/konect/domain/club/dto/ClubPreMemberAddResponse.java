package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubPreMemberAddResponse(
    @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer clubId,

    @Schema(description = "학번", example = "2021136089", requiredMode = REQUIRED)
    String studentNumber,

    @Schema(description = "이름", example = "홍길동", requiredMode = REQUIRED)
    String name,

    @Schema(description = "직접 회원 가입 여부 (true: 이미 앱 가입자라 ClubMember에 직접 추가됨, false: ClubPreMember에 사전등록됨)",
        example = "false", requiredMode = REQUIRED)
    Boolean isDirectMember
) {
    public static ClubPreMemberAddResponse from(ClubPreMember preMember) {
        return new ClubPreMemberAddResponse(
            preMember.getClub().getId(),
            preMember.getStudentNumber(),
            preMember.getName(),
            false
        );
    }

    public static ClubPreMemberAddResponse from(ClubMember clubMember) {
        return new ClubPreMemberAddResponse(
            clubMember.getClub().getId(),
            clubMember.getUser().getStudentNumber(),
            clubMember.getUser().getName(),
            true
        );
    }
}
