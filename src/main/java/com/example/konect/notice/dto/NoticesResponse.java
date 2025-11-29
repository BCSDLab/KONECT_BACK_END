package com.example.konect.notice.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import org.springframework.data.domain.Page;

import com.example.konect.notice.model.Notice;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoticesResponse(
    @Schema(description = "조건에 해당하는 공지사항 수", example = "10", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 공지사항 수", example = "5", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "2", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "공지사항 리스트", requiredMode = REQUIRED)
    List<InnerNoticeResponse> notices
) {
    public record InnerNoticeResponse(
        @Schema(description = "공지사항 고유 id", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "공지사항 제목", example = "동아리 박람회 참가 신청 마감 안내", requiredMode = REQUIRED)
        String title
    ) {
        public static InnerNoticeResponse from(Notice notice) {
            return new InnerNoticeResponse(notice.getId(), notice.getTitle());
        }
    }

    public static NoticesResponse from(Page<Notice> page) {
        return new NoticesResponse(
            page.getTotalElements(),
            page.getNumberOfElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            page.stream()
                .map(NoticesResponse.InnerNoticeResponse::from)
                .toList()
        );
    }
}
