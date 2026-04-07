package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record SheetImportPreviewResponse(
    @Schema(description = "Total preview member count", example = "10", requiredMode = REQUIRED)
    int previewCount,

    @Schema(description = "Members that will be registered directly", example = "2", requiredMode = REQUIRED)
    int autoRegisteredCount,

    @Schema(description = "Members that will be pre-registered", example = "8", requiredMode = REQUIRED)
    int preRegisteredCount,

    @Schema(description = "Preview member list", requiredMode = REQUIRED)
    List<PreviewMember> members,

    @Schema(description = "Warnings collected during preview", requiredMode = REQUIRED)
    List<String> warnings
) {
    public record PreviewMember(
        @Schema(description = "Student number", example = "2021136089", requiredMode = REQUIRED)
        String studentNumber,

        @Schema(description = "Member name", example = "Kim Konect", requiredMode = REQUIRED)
        String name,

        @Schema(description = "Club position to register", example = "MEMBER", requiredMode = REQUIRED)
        ClubPosition clubPosition,

        @Schema(description = "True when the member will be registered directly as ClubMember",
            example = "false", requiredMode = REQUIRED)
        Boolean isDirectMember
    ) {
        public static PreviewMember from(ClubMember clubMember) {
            return new PreviewMember(
                clubMember.getUser().getStudentNumber(),
                clubMember.getUser().getName(),
                clubMember.getClubPosition(),
                true
            );
        }

        public static PreviewMember from(ClubPreMember preMember) {
            return new PreviewMember(
                preMember.getStudentNumber(),
                preMember.getName(),
                preMember.getClubPosition(),
                false
            );
        }
    }

    public static SheetImportPreviewResponse of(
        List<PreviewMember> members,
        List<String> warnings
    ) {
        List<PreviewMember> safeMembers = members != null ? members : List.of();
        List<String> safeWarnings = warnings != null ? warnings : List.of();

        int autoRegisteredCount = (int)safeMembers.stream()
            .filter(member -> Boolean.TRUE.equals(member.isDirectMember()))
            .count();
        int preRegisteredCount = safeMembers.size() - autoRegisteredCount;

        return new SheetImportPreviewResponse(
            safeMembers.size(),
            autoRegisteredCount,
            preRegisteredCount,
            safeMembers,
            safeWarnings
        );
    }
}
