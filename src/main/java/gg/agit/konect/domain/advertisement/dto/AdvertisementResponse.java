package gg.agit.konect.domain.advertisement.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.advertisement.model.Advertisement;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdvertisementResponse(
        @Schema(description = "광고 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "광고 제목", example = "개발자pick", requiredMode = REQUIRED)
        String title,

        @Schema(description = "광고 설명", example = "부회장이 추천하는 노트북 LG Gram", requiredMode = REQUIRED)
        String description,

        @Schema(description = "광고 이미지 URL", example = "https://example.com/advertisement.png", requiredMode = REQUIRED)
        String imageUrl,

        @Schema(description = "광고 링크 URL", example = "https://www.example.com", requiredMode = REQUIRED)
        String linkUrl
) {
    public static AdvertisementResponse from(Advertisement advertisement) {
        return new AdvertisementResponse(
                advertisement.getId(),
                advertisement.getTitle(),
                advertisement.getDescription(),
                advertisement.getImageUrl(),
                advertisement.getLinkUrl()
        );
    }
}
