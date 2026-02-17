package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.ClubPreMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubPreMembersResponse(
    @Schema(description = "동아리 사전 회원 리스트", requiredMode = REQUIRED)
    List<InnerClubPreMember> preMembers
) {
    public record InnerClubPreMember(
        @Schema(description = "사전 회원 ID", example = "1", requiredMode = REQUIRED)
        Integer preMemberId,

        @Schema(description = "학번", example = "2021136089", requiredMode = REQUIRED)
        String studentNumber,

        @Schema(description = "이름", example = "홍길동", requiredMode = REQUIRED)
        String name,

        @Schema(description = "가입 직책", example = "MEMBER", requiredMode = REQUIRED)
        ClubPosition clubPosition
    ) {
        public static InnerClubPreMember from(ClubPreMember preMember) {
            return new InnerClubPreMember(
                preMember.getId(),
                preMember.getStudentNumber(),
                preMember.getName(),
                preMember.getClubPosition()
            );
        }
    }

    public static ClubPreMembersResponse from(List<ClubPreMember> preMembers) {
        return new ClubPreMembersResponse(
            preMembers.stream()
                .map(InnerClubPreMember::from)
                .toList()
        );
    }
}
