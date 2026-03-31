package gg.agit.konect.domain.chat.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRoomMatchesResponse(
    @Schema(description = "조건에 해당하는 채팅방 총 개수", example = "10", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 채팅방 개수", example = "5", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "2", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "채팅방 이름으로 매칭된 채팅방 목록", requiredMode = REQUIRED)
    List<ChatRoomSummaryResponse> rooms
) {

    public static ChatRoomMatchesResponse from(Page<ChatRoomSummaryResponse> page) {
        return new ChatRoomMatchesResponse(
            page.getTotalElements(),
            page.getNumberOfElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            page.getContent()
        );
    }
}
