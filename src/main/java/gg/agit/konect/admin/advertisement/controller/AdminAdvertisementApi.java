package gg.agit.konect.admin.advertisement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementCreateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementResponse;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementUpdateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementsResponse;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.auth.annotation.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Admin) Advertisement: 광고", description = "어드민 광고 API")
@RequestMapping("/admin/advertisements")
public interface AdminAdvertisementApi {

    @Operation(summary = "광고 목록을 조회한다.")
    @GetMapping
    ResponseEntity<AdminAdvertisementsResponse> getAdvertisements();

    @Operation(summary = "광고 단건을 조회한다.")
    @GetMapping("/{id}")
    ResponseEntity<AdminAdvertisementResponse> getAdvertisement(@PathVariable Integer id);

    @Operation(summary = "광고를 생성한다.")
    @PostMapping
    ResponseEntity<Void> createAdvertisement(@Valid @RequestBody AdminAdvertisementCreateRequest request);

    @Operation(summary = "광고를 수정한다.")
    @PutMapping("/{id}")
    ResponseEntity<Void> updateAdvertisement(
        @PathVariable Integer id,
        @Valid @RequestBody AdminAdvertisementUpdateRequest request
    );

    @Operation(summary = "광고를 삭제한다.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteAdvertisement(@PathVariable Integer id);
}
