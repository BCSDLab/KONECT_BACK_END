package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMemberApplicationAnswersResponse(
    @Schema(description = "지원서 총 개수", example = "3", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "승인된 회원 지원서 목록", requiredMode = REQUIRED)
    List<ClubApplicationAnswersResponse> applications
) {
    public static ClubMemberApplicationAnswersResponse from(List<ClubApplicationAnswersResponse> applications) {
        return new ClubMemberApplicationAnswersResponse(
            (long)applications.size(),
            applications
        );
    }
}
