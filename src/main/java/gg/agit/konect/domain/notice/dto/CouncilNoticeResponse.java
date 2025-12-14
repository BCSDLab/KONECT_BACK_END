package gg.agit.konect.domain.notice.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.notice.model.CouncilNotice;
import io.swagger.v3.oas.annotations.media.Schema;

public record CouncilNoticeResponse(
    @Schema(description = "공지사항 고유 id", example = "1", requiredMode = REQUIRED)
    Integer id,

    @Schema(description = "공지사항 제목", example = "동아리 박람회 참가 신청 마감 안내", requiredMode = REQUIRED)
    String title,

    @Schema(description = "공지사항 내용", example = "2025년 동아리 박람회 참가 신청이 12월 15일까지입니다.", requiredMode = REQUIRED)
    String content,

    @Schema(description = "공지사항 생성 일시", example = "2025-12-03T10:30:00", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    LocalDateTime createdAt,

    @Schema(description = "공지사항 수정 일시", example = "2025-12-03T10:30:00", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    LocalDateTime updatedAt
) {
    public static CouncilNoticeResponse from(CouncilNotice councilNotice) {
        return new CouncilNoticeResponse(
            councilNotice.getId(),
            councilNotice.getTitle(),
            councilNotice.getContent(),
            councilNotice.getCreatedAt(),
            councilNotice.getUpdatedAt()
        );
    }
}
