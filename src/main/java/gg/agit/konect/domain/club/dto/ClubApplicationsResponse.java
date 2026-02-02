package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import gg.agit.konect.domain.club.model.ClubApply;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubApplicationsResponse(
    @Schema(description = "조건에 해당하는 지원 내역 총 개수", example = "10", requiredMode = REQUIRED)
    Long totalCount,

    @Schema(description = "현재 페이지에서 조회된 지원 내역 개수", example = "5", requiredMode = REQUIRED)
    Integer currentCount,

    @Schema(description = "최대 페이지", example = "2", requiredMode = REQUIRED)
    Integer totalPage,

    @Schema(description = "현재 페이지", example = "1", requiredMode = REQUIRED)
    Integer currentPage,

    @Schema(description = "동아리 지원 내역 리스트", requiredMode = REQUIRED)
    List<ClubApplicationResponse> applications
) {

    public record ClubApplicationResponse(
        @Schema(description = "지원 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "지원자 학번", example = "20250120", requiredMode = REQUIRED)
        String studentNumber,

        @Schema(description = "지원자 이름", example = "이동훈", requiredMode = REQUIRED)
        String name,

        @Schema(description = "지원자 프로필 이미지 링크", example = "https://stage-static.koreatech.in/konect/User_02.png", requiredMode = REQUIRED)
        String imageUrl,

        @Schema(description = "지원 일시", example = "2025-01-13T10:30:00", requiredMode = REQUIRED)
        LocalDateTime appliedAt
    ) {

        public static ClubApplicationResponse from(ClubApply clubApply) {
            return new ClubApplicationResponse(
                clubApply.getId(),
                clubApply.getUser().getStudentNumber(),
                clubApply.getUser().getName(),
                clubApply.getUser().getImageUrl(),
                clubApply.getCreatedAt()
            );
        }
    }

    public static ClubApplicationsResponse from(Page<ClubApply> page) {
        return new ClubApplicationsResponse(
            page.getTotalElements(),
            page.getNumberOfElements(),
            page.getTotalPages(),
            page.getNumber() + 1,
            page.getContent().stream()
                .map(ClubApplicationResponse::from)
                .toList()
        );
    }
}
