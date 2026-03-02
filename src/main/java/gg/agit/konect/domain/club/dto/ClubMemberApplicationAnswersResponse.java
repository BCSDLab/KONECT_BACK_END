package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import org.springframework.data.domain.Page;

import gg.agit.konect.domain.club.model.ClubApply;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMemberApplicationAnswersResponse(
    @Schema(description = "지원서 총 개수", example = "3", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 지원서 개수", example = "3", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "2", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "승인된 회원 지원서 목록", requiredMode = REQUIRED)
    List<ClubApplicationAnswersResponse> applications
) {
    public static ClubMemberApplicationAnswersResponse from(
        Page<ClubApply> page,
        List<ClubApplicationAnswersResponse> applications
    ) {
        return new ClubMemberApplicationAnswersResponse(
            page.getTotalElements(),
            page.getNumberOfElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            applications
        );
    }
}
