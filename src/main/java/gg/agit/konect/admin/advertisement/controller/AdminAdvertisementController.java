package gg.agit.konect.admin.advertisement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementCreateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementResponse;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementUpdateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementsResponse;
import gg.agit.konect.admin.advertisement.service.AdminAdvertisementService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.auth.annotation.Auth;
import jakarta.validation.Valid;

@RestController
@Validated
@Auth(roles = {UserRole.ADMIN})
public class AdminAdvertisementController implements AdminAdvertisementApi {

    private final AdminAdvertisementService adminAdvertisementService;

    public AdminAdvertisementController(AdminAdvertisementService adminAdvertisementService) {
        this.adminAdvertisementService = adminAdvertisementService;
    }

    @Override
    public ResponseEntity<AdminAdvertisementsResponse> getAdvertisements() {
        AdminAdvertisementsResponse response = adminAdvertisementService.getAdvertisements();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AdminAdvertisementResponse> getAdvertisement(@PathVariable Integer id) {
        AdminAdvertisementResponse response = adminAdvertisementService.getAdvertisement(id);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> createAdvertisement(@Valid @RequestBody AdminAdvertisementCreateRequest request) {
        adminAdvertisementService.createAdvertisement(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateAdvertisement(
        @PathVariable Integer id,
        @Valid @RequestBody AdminAdvertisementUpdateRequest request
    ) {
        adminAdvertisementService.updateAdvertisement(id, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteAdvertisement(@PathVariable Integer id) {
        adminAdvertisementService.deleteAdvertisement(id);
        return ResponseEntity.ok().build();
    }
}
