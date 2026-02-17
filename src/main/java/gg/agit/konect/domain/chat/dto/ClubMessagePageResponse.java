package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMessagePageResponse(
    @Schema(description = "조건에 해당하는 메시지 수", example = "100", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 메시지 수", example = "20", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "5", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "동아리 ID", example = "1", requiredMode = NOT_REQUIRED)
    Integer clubId,

    @Schema(description = "채팅 메시지 리스트", requiredMode = REQUIRED)
    List<ClubMessageResponse> messages
) {
}
