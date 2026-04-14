package gg.agit.konect.domain.event.dto;

import java.util.List;

public record EventBoothMapResponse(
    String mapImageUrl,
    List<ZoneResponse> zones,
    List<BoothMapItemResponse> booths
) {

    public record ZoneResponse(
        String code,
        String label
    ) {
    }

    public record BoothMapItemResponse(
        Integer boothId,
        String name,
        String zone,
        Integer x,
        Integer y,
        Integer width,
        Integer height,
        String status
    ) {
    }
}
