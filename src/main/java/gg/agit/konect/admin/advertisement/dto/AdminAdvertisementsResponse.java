package gg.agit.konect.admin.advertisement.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.advertisement.model.Advertisement;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminAdvertisementsResponse(
        @Schema(description = "광고 리스트", requiredMode = REQUIRED)
        List<AdminAdvertisementResponse> advertisements
) {
    public static AdminAdvertisementsResponse from(List<Advertisement> advertisements) {
        return new AdminAdvertisementsResponse(
                advertisements.stream()
                        .map(AdminAdvertisementResponse::from)
                        .toList()
        );
    }
}
