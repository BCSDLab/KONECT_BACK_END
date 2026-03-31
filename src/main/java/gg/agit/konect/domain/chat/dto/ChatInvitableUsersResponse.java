package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.chat.enums.ChatInviteSortBy;
import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatInvitableUsersResponse(
    @Schema(description = "정렬 기준", example = "CLUB", requiredMode = REQUIRED)
    ChatInviteSortBy sortBy,

    @Schema(description = "동아리 섹션 그룹핑 여부", example = "true", requiredMode = REQUIRED)
    boolean grouped,

    @Schema(description = "이름순 정렬일 때 반환되는 초대 가능 사용자 리스트", requiredMode = REQUIRED)
    List<InvitableUser> users,

    @Schema(description = "동아리순 정렬일 때 반환되는 섹션 리스트", requiredMode = REQUIRED)
    List<InvitableSection> sections
) {

    public record InvitableUser(
        @Schema(description = "유저 ID", example = "1", requiredMode = REQUIRED)
        Integer userId,

        @Schema(description = "이름", example = "최승운", requiredMode = REQUIRED)
        String name,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png", requiredMode = NOT_REQUIRED)
        String imageUrl,

        @Schema(description = "학번", example = "2021234567", requiredMode = REQUIRED)
        String studentNumber
    ) {
        public static InvitableUser from(User user) {
            return new InvitableUser(
                user.getId(),
                user.getName(),
                user.getImageUrl(),
                user.getStudentNumber()
            );
        }
    }

    public record InvitableSection(
        @Schema(description = "동아리 ID, 기타 섹션이면 null", example = "3", requiredMode = NOT_REQUIRED)
        Integer clubId,

        @Schema(description = "섹션 이름", example = "BCSD", requiredMode = REQUIRED)
        String clubName,

        @Schema(description = "해당 섹션의 초대 가능 사용자 리스트", requiredMode = REQUIRED)
        List<InvitableUser> users
    ) {
    }

    public static ChatInvitableUsersResponse forNameSort(List<InvitableUser> users) {
        return new ChatInvitableUsersResponse(ChatInviteSortBy.NAME, false, users, List.of());
    }

    public static ChatInvitableUsersResponse forClubSort(List<InvitableSection> sections) {
        return new ChatInvitableUsersResponse(ChatInviteSortBy.CLUB, true, List.of(), sections);
    }
}
