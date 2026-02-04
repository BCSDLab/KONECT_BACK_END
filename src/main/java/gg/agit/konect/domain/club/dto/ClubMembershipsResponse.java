package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.model.ClubMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMembershipsResponse(
    @Schema(description = "가입한 동아리 리스트", requiredMode = REQUIRED)
    List<InnerJoinedClubResponse> joinedClubs
) {
    public record InnerJoinedClubResponse(
        @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "동아리 이름", example = "BCSD", requiredMode = REQUIRED)
        String name,

        @Schema(description = "동아리 이미지 링크", example = "https://bcsdlab.com/static/img/logo.d89d9cc.png", requiredMode = REQUIRED)
        String imageUrl,

        @Schema(description = "동아리 분과", example = "학술", requiredMode = REQUIRED)
        String categoryName,

        @Schema(description = "직책", example = "회장", requiredMode = REQUIRED)
        String position,

        @Schema(description = "회비 완납 여부", example = "true", requiredMode = REQUIRED)
        Boolean isFeePaid
    ) {
        public static InnerJoinedClubResponse from(ClubMember clubMember) {
            return new InnerJoinedClubResponse(
                clubMember.getClub().getId(),
                clubMember.getClub().getName(),
                clubMember.getClub().getImageUrl(),
                clubMember.getClub().getClubCategory().getDescription(),
                clubMember.getClubPosition().getDescription(),
                clubMember.getIsFeePaid()
            );
        }
    }

    public static ClubMembershipsResponse from(List<ClubMember> clubMembers) {
        return new ClubMembershipsResponse(clubMembers.stream()
            .map(InnerJoinedClubResponse::from)
            .toList());
    }
}
