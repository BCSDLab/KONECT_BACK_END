package gg.agit.konect.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;
import java.util.Map;

import gg.agit.konect.club.enums.PositionGroup;
import gg.agit.konect.club.model.ClubMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record JoinedClubsResponse(
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

        @Schema(description = "직책 그룹", example = "PRESIDENT", requiredMode = REQUIRED)
        PositionGroup positionGroup,

        @Schema(description = "미납 회비 금액", example = "10000", requiredMode = REQUIRED)
        Integer unpaidFeeAmount
    ) {
        public static InnerJoinedClubResponse of(ClubMember clubMember, Integer unpaidFeeAmount) {
            return new InnerJoinedClubResponse(
                clubMember.getClub().getId(),
                clubMember.getClub().getName(),
                clubMember.getClub().getImageUrl(),
                clubMember.getClub().getClubCategory().getName(),
                clubMember.getClubPosition().getName(),
                clubMember.getClubPosition().getClubPositionGroup().getName(),
                unpaidFeeAmount
            );
        }
    }

    public static JoinedClubsResponse of(List<ClubMember> clubMembers, Map<Integer, Integer> unpaidFeeAmountMap) {
        return new JoinedClubsResponse(clubMembers.stream()
            .map(clubMember -> InnerJoinedClubResponse.of(clubMember,
                unpaidFeeAmountMap.get(clubMember.getClub().getId())
            ))
            .toList());
    }
}
