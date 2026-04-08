package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubPreMemberBatchAddResponse(
    @Schema(description = "전체 요청 수", example = "10", requiredMode = REQUIRED)
    Integer totalCount,

    @Schema(description = "성공한 회원 수", example = "8", requiredMode = REQUIRED)
    Integer successCount,

    @Schema(description = "실패한 회원 수", example = "2", requiredMode = REQUIRED)
    Integer failedCount,

    @Schema(description = "개별 처리 결과 목록", requiredMode = REQUIRED)
    List<ClubPreMemberBatchResultItem> results
) {

    public static ClubPreMemberBatchAddResponse from(List<ClubPreMemberBatchResultItem> results) {
        int totalCount = results.size();
        int successCount = (int)results.stream()
            .filter(ClubPreMemberBatchResultItem::success)
            .count();
        int failedCount = totalCount - successCount;

        return new ClubPreMemberBatchAddResponse(totalCount, successCount, failedCount, results);
    }
}
