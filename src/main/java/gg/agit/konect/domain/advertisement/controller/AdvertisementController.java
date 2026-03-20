package gg.agit.konect.domain.advertisement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<AdvertisementsResponse> getAdvertisements(int count) {
        AdvertisementsResponse response = advertisementService.getRandomAdvertisements(count);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> increaseClickCount(@PathVariable Integer id) {
        advertisementService.increaseClickCount(id);
        return ResponseEntity.noContent().build();
    }
}
