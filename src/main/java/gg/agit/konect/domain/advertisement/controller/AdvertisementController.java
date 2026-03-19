package gg.agit.konect.domain.advertisement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.advertisement.dto.AdvertisementResponse;
import gg.agit.konect.domain.advertisement.dto.AdvertisementsResponse;
import gg.agit.konect.domain.advertisement.service.AdvertisementService;

@RestController
@Validated
public class AdvertisementController implements AdvertisementApi {

    private final AdvertisementService advertisementService;

    public AdvertisementController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @Override
    public ResponseEntity<AdvertisementsResponse> getAdvertisements() {
        AdvertisementsResponse response = advertisementService.getVisibleAdvertisements();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AdvertisementResponse> getAdvertisement(@PathVariable Integer id) {
        AdvertisementResponse response = advertisementService.getVisibleAdvertisement(id);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> increaseClickCount(@PathVariable Integer id) {
        advertisementService.increaseClickCount(id);
        return ResponseEntity.noContent().build();
    }
}
