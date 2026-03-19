package gg.agit.konect.domain.advertisement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.advertisement.dto.AdvertisementResponse;
import gg.agit.konect.domain.advertisement.dto.AdvertisementsResponse;
import gg.agit.konect.global.auth.annotation.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Advertisement: 광고", description = "광고 API")
@RequestMapping("/advertisements")
public interface AdvertisementApi {

    @PublicApi
    @Operation(summary = "노출 가능한 광고 목록을 조회한다.")
    @GetMapping
    ResponseEntity<AdvertisementsResponse> getAdvertisements();

    @PublicApi
    @Operation(summary = "노출 가능한 광고 단건을 조회한다.")
    @GetMapping("/{id}")
    ResponseEntity<AdvertisementResponse> getAdvertisement(@PathVariable Integer id);

    @PublicApi
    @Operation(summary = "광고 클릭 수를 증가시킨다.")
    @PostMapping("/{id}/clicks")
    ResponseEntity<Void> increaseClickCount(@PathVariable Integer id);
}
