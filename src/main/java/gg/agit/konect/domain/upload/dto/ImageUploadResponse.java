package gg.agit.konect.domain.upload.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUploadResponse(
    @Schema(description = "S3 object key", example = "konect/2026/01/26/550e8400-e29b-41d4-a716-446655440000.png",
        requiredMode = REQUIRED)
    String key,

    @Schema(description = "CloudFront를 통한 접근 URL", requiredMode = REQUIRED)
    String fileUrl
) {

}
