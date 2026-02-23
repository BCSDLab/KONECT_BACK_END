package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMembersResponse(
    @Schema(description = "동아리 멤버 리스트", requiredMode = REQUIRED)
    List<InnerClubMember> clubMembers
) {
    public record InnerClubMember(
        @Schema(description = "유저 ID", example = "1", requiredMode = REQUIRED)
        Integer userId,

        @Schema(description = "동아리 맴버 이름", example = "배진호", requiredMode = REQUIRED)
        String name,

        @Schema(description = "동아리 멤버 프로필 사진", example = "https://bcsdlab.com/static/img/logo.d89d9cc.png", requiredMode = REQUIRED)
        String imageUrl,

        @Schema(description = "마스킹된 동아리 멤버 학번", example = "*******061", requiredMode = REQUIRED)
        String studentNumber,

        @Schema(description = "직책", example = "PRESIDENT", requiredMode = REQUIRED)
        ClubPosition position
    ) {
        private static final int STUDENT_NUMBER_VISIBLE_LENGTH = 3;

        public static InnerClubMember from(ClubMember clubMember) {
            User user = clubMember.getUser();

            return new InnerClubMember(
                user.getId(),
                user.getName(),
                user.getImageUrl(),
                maskStudentNumber(user.getStudentNumber()),
                clubMember.getClubPosition()
            );
        }

        private static String maskStudentNumber(String studentNumber) {
            if (studentNumber == null || studentNumber.length() <= STUDENT_NUMBER_VISIBLE_LENGTH) {
                return studentNumber;
            }

            int maskedLength = studentNumber.length() - STUDENT_NUMBER_VISIBLE_LENGTH;
            return "*".repeat(maskedLength)
                + studentNumber.substring(maskedLength);
        }
    }

    public static ClubMembersResponse from(List<ClubMember> clubMembers) {
        return new ClubMembersResponse(
            clubMembers.stream()
                .map(InnerClubMember::from)
                .toList()
        );
    }
}
