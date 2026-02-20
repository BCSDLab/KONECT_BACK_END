package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubRecruitmentImage;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubRecruitmentResponse(
    @Schema(description = "모집 공고 ID", example = "1", requiredMode = REQUIRED)
    Integer id,

    @Schema(description = "동아리 ID", example = "1", requiredMode = REQUIRED)
    Integer clubId,

    @Schema(description = "모집 상태", example = "ONGOING", requiredMode = REQUIRED)
    RecruitmentStatus status,

    @Schema(description = "모집 시작일", example = "2025.11.30", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd")
    LocalDate startDate,

    @Schema(description = "모집 마감일", example = "2025.12.31", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd")
    LocalDate endDate,

    @Schema(description = "모집 공고 내용", example = "BCSD 2025학년도 2학기 신입 부원 모집...", requiredMode = REQUIRED)
    String content,

    @Schema(description = "모집 공고 이미지 리스트", requiredMode = REQUIRED)
    List<InnerRecruitmentImage> images,

    @Schema(description = "지원 여부", example = "false", requiredMode = REQUIRED)
    Boolean isApplied,

    @Schema(description = "회비 납부 필요 여부", example = "true", requiredMode = REQUIRED)
    Boolean isFeeRequired
) {
    public record InnerRecruitmentImage(
        @Schema(description = "모집 공고 이미지 URL", example = "https://example.com/image.png", requiredMode = NOT_REQUIRED)
        String url
    ) {
        public static InnerRecruitmentImage from(ClubRecruitmentImage clubRecruitmentImage) {
            return new InnerRecruitmentImage(clubRecruitmentImage.getUrl());
        }
    }

    public static ClubRecruitmentResponse of(ClubRecruitment recruitment, Boolean isApplied) {
        return new ClubRecruitmentResponse(
            recruitment.getId(),
            recruitment.getClub().getId(),
            RecruitmentStatus.of(recruitment),
            recruitment.getStartDate(),
            recruitment.getEndDate(),
            recruitment.getContent(),
            recruitment.getImages().stream()
                .map(InnerRecruitmentImage::from)
                .toList(),
            isApplied,
            recruitment.getClub().getIsFeeRequired()
        );
    }
}
