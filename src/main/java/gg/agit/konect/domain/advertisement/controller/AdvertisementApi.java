package gg.agit.konect.domain.advertisement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.advertisement.dto.AdvertisementsResponse;
import gg.agit.konect.global.auth.annotation.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(name = "(Normal) Advertisement: 광고", description = "광고 API")
@RequestMapping("/advertisements")
public interface AdvertisementApi {

    @PublicApi
    @Operation(summary = "노출 가능한 광고를 랜덤으로 조회한다. 필요로 하는 수가 더 큰 경우 중복 허용.")
    @GetMapping
    ResponseEntity<AdvertisementsResponse> getAdvertisements(
        @Parameter(description = "조회할 광고 개수 (1~10)", example = "1")
        @RequestParam(defaultValue = "1") @Min(1) @Max(10) int count
    );

    @PublicApi
    @Operation(summary = "광고 클릭 수를 증가시킨다.")
    @PostMapping("/{id}/clicks")
    ResponseEntity<Void> increaseClickCount(@PathVariable Integer id);
}
