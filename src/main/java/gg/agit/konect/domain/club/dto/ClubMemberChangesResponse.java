package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.model.ClubMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMemberChangesResponse(
    @Schema(description = "변경된 회원 목록", requiredMode = REQUIRED)
    List<ClubMemberResponse> changedMembers
) {
    public static ClubMemberChangesResponse from(List<ClubMember> clubMembers) {
        return new ClubMemberChangesResponse(
            clubMembers.stream()
                .map(ClubMemberResponse::from)
                .toList()
        );
    }

    public static ClubMemberChangesResponse of(ClubMember... clubMembers) {
        return from(List.of(clubMembers));
    }
}
