package com.example.konect.notice.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.konect.notice.model.CouncilNotice;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

public record CouncilNoticesResponse(
    @Schema(description = "조건에 해당하는 공지사항 수", example = "10", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 공지사항 수", example = "5", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "2", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "총학생회 공지사항 리스트", requiredMode = REQUIRED)
    List<InnerCouncilNoticeResponse> notices
) {
    public record InnerCouncilNoticeResponse(
        @Schema(description = "공지사항 고유 id", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "공지사항 제목", example = "동아리 박람회 참가 신청 마감 안내", requiredMode = REQUIRED)
        String title,

        @Schema(description = "공지사항 생성 일자", example = "2025.11.30", requiredMode = REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd")
        LocalDate createdAt

    ) {
        public static InnerCouncilNoticeResponse from(CouncilNotice councilNotice) {
            return new InnerCouncilNoticeResponse(
                councilNotice.getId(), councilNotice.getTitle(), councilNotice.getCreatedAt().toLocalDate());
        }
    }

    public static CouncilNoticesResponse from(Page<CouncilNotice> page) {
        return new CouncilNoticesResponse(
            page.getTotalElements(),
            page.getNumberOfElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            page.stream()
                .map(InnerCouncilNoticeResponse::from)
                .toList()
        );
    }
}
