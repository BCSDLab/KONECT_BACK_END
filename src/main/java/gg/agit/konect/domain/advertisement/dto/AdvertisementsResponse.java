package gg.agit.konect.domain.advertisement.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.advertisement.model.Advertisement;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdvertisementsResponse(
    @Schema(description = "광고 리스트", requiredMode = REQUIRED)
    List<AdvertisementResponse> advertisements
) {
    public static AdvertisementsResponse from(List<Advertisement> advertisements) {
        return new AdvertisementsResponse(
            advertisements.stream()
                .map(AdvertisementResponse::from)
                .toList()
        );
    }
}
