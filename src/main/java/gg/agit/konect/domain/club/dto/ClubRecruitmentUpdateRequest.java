package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ClubRecruitmentUpdateRequest(
    @Schema(description = "모집 시작일", example = "2025.11.30", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd")
    LocalDate startDate,

    @Schema(description = "모집 마감일", example = "2025.12.31", requiredMode = REQUIRED)
    @JsonFormat(pattern = "yyyy.MM.dd")
    LocalDate endDate,

    @NotNull(message = "상시 모집 여부는 필수 입력입니다.")
    @Schema(description = "상시 모집 여부", example = "false", requiredMode = REQUIRED)
    Boolean isAlwaysRecruiting,

    @NotEmpty(message = "모집 공고 내용은 필수 입력입니다.")
    @Schema(description = "모집 공고 내용", example = "BCSD 2025학년도 2학기 신입 부원 모집...", requiredMode = REQUIRED)
    String content,

    @NotNull(message = "모집 공고 이미지 리스트는 필수 입력입니다.")
    @Valid
    @Schema(description = "모집 공고 이미지 리스트", requiredMode = REQUIRED)
    List<InnerClubRecruitmentImageRequest> images
) {
    public record InnerClubRecruitmentImageRequest(
        @NotEmpty(message = "모집 공고 이미지 URL은 필수 입력입니다.")
        @Schema(description = "모집 공고 이미지 URL", example = "https://example.com/image.png", requiredMode = REQUIRED)
        String url
    ) {

    }

    public List<String> getImageUrls() {
        return images.stream()
            .map(InnerClubRecruitmentImageRequest::url)
            .toList();
    }
}
