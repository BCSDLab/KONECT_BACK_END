package gg.agit.konect.admin.advertisement.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.advertisement.model.Advertisement;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminAdvertisementResponse(
    @Schema(description = "광고 ID", example = "1", requiredMode = REQUIRED)
    Integer id,

    @Schema(description = "광고 제목", example = "개발자pick", requiredMode = REQUIRED)
    String title,

    @Schema(description = "광고 설명", example = "부회장이 추천하는 노트북 LG Gram", requiredMode = REQUIRED)
    String description,

    @Schema(description = "광고 이미지 URL", example = "https://example.com/advertisement.png", requiredMode = REQUIRED)
    String imageUrl,

    @Schema(description = "광고 링크 URL", example = "https://www.example.com", requiredMode = REQUIRED)
    String linkUrl,

    @Schema(description = "광고 노출 여부", example = "true", requiredMode = REQUIRED)
    Boolean isVisible,

    @Schema(description = "광고 클릭 수", example = "3", requiredMode = REQUIRED)
    Integer clickCount,

    @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
    @Schema(description = "생성 일시", example = "2026.03.18 14:00", requiredMode = REQUIRED)
    LocalDateTime createdAt,

    @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
    @Schema(description = "수정 일시", example = "2026.03.18 14:00", requiredMode = REQUIRED)
    LocalDateTime updatedAt
) {
    public static AdminAdvertisementResponse from(Advertisement advertisement) {
        return new AdminAdvertisementResponse(
            advertisement.getId(),
            advertisement.getTitle(),
            advertisement.getDescription(),
            advertisement.getImageUrl(),
            advertisement.getLinkUrl(),
            advertisement.getIsVisible(),
            advertisement.getClickCount(),
            advertisement.getCreatedAt(),
            advertisement.getUpdatedAt()
        );
    }
}
