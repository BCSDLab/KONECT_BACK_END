package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record SheetImportPreviewResponse(
    @Schema(description = "전체 미리보기 인원 수", example = "10", requiredMode = REQUIRED)
    int previewCount,

    @Schema(description = "즉시 등록될 인원 수", example = "2", requiredMode = REQUIRED)
    int autoRegisteredCount,

    @Schema(description = "사전 등록될 인원 수", example = "8", requiredMode = REQUIRED)
    int preRegisteredCount,

    @Schema(description = "미리보기 부원 목록", requiredMode = REQUIRED)
    List<PreviewMember> members,

    @Schema(description = "미리보기 중 수집된 경고 목록", requiredMode = REQUIRED)
    List<String> warnings
) {
    public record PreviewMember(
        @Schema(description = "학번", example = "2021136089", requiredMode = REQUIRED)
        String studentNumber,

        @Schema(description = "이름", example = "김코넥트", requiredMode = REQUIRED)
        String name,

        @Schema(description = "등록할 동아리 직책", example = "MEMBER", requiredMode = REQUIRED)
        ClubPosition clubPosition,

        @Schema(description = "ClubMember로 즉시 등록되는 경우 true", example = "false", requiredMode = REQUIRED)
        Boolean isDirectMember,

        @Schema(description = "최종 등록 대상이면 true", example = "true", requiredMode = REQUIRED)
        Boolean enabled
    ) {
        public static PreviewMember from(ClubMember clubMember) {
            return new PreviewMember(
                clubMember.getUser().getStudentNumber(),
                clubMember.getUser().getName(),
                clubMember.getClubPosition(),
                true,
                true
            );
        }

        public static PreviewMember from(ClubPreMember preMember) {
            return new PreviewMember(
                preMember.getStudentNumber(),
                preMember.getName(),
                preMember.getClubPosition(),
                false,
                true
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
