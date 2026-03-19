package gg.agit.konect.domain.advertisement.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_ADVERTISEMENT;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.advertisement.dto.AdvertisementResponse;
import gg.agit.konect.domain.advertisement.dto.AdvertisementsResponse;
import gg.agit.konect.domain.advertisement.model.Advertisement;
import gg.agit.konect.domain.advertisement.repository.AdvertisementRepository;
import gg.agit.konect.global.exception.CustomException;

@Service
@Transactional(readOnly = true)
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;

    public AdvertisementService(AdvertisementRepository advertisementRepository) {
        this.advertisementRepository = advertisementRepository;
    }

    public AdvertisementsResponse getVisibleAdvertisements() {
        return AdvertisementsResponse.from(advertisementRepository.findAllByIsVisibleTrueOrderByCreatedAtDesc());
    }

    public AdvertisementResponse getVisibleAdvertisement(Integer id) {
        Advertisement advertisement = advertisementRepository.getById(id);

        if (!Boolean.TRUE.equals(advertisement.getIsVisible())) {
            throw CustomException.of(NOT_FOUND_ADVERTISEMENT);
        }

        return AdvertisementResponse.from(advertisement);
    }

    @Transactional
    public void increaseClickCount(Integer id) {
        int updatedCount = advertisementRepository.incrementClickCount(id);
        if (updatedCount == 0) {
            throw CustomException.of(NOT_FOUND_ADVERTISEMENT);
        }
    }
}
