package gg.agit.konect.admin.advertisement.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAdvertisementCreateRequest(
        @NotBlank(message = "광고 제목은 필수 입력입니다.")
        @Size(max = 100, message = "광고 제목은 100자를 초과할 수 없습니다.")
        @Schema(description = "광고 제목", example = "개발자pick", requiredMode = REQUIRED)
        String title,

        @NotBlank(message = "광고 설명은 필수 입력입니다.")
        @Size(max = 255, message = "광고 설명은 255자를 초과할 수 없습니다.")
        @Schema(description = "광고 설명", example = "부회장이 추천하는 노트북 LG Gram", requiredMode = REQUIRED)
        String description,

        @NotBlank(message = "광고 이미지는 필수 입력입니다.")
        @Size(max = 255, message = "광고 이미지 URL은 255자를 초과할 수 없습니다.")
        @Schema(description = "광고 이미지 URL", example = "https://example.com/advertisement.png", requiredMode = REQUIRED)
        String imageUrl,

        @NotBlank(message = "광고 링크는 필수 입력입니다.")
        @Size(max = 255, message = "광고 링크 URL은 255자를 초과할 수 없습니다.")
        @Schema(description = "광고 링크 URL", example = "https://www.example.com", requiredMode = REQUIRED)
        String linkUrl,

        @NotNull(message = "광고 노출 여부는 필수 입력입니다.")
        @Schema(description = "광고 노출 여부", example = "true", requiredMode = REQUIRED)
        Boolean isVisible
) {
}
