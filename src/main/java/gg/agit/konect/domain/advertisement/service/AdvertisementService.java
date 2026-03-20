package gg.agit.konect.domain.advertisement.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_ADVERTISEMENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AdvertisementsResponse getRandomAdvertisements(int count) {
        List<Integer> visibleIds = advertisementRepository.findAllVisibleIds();

        if (visibleIds.isEmpty()) {
            return AdvertisementsResponse.from(List.of());
        }

        List<Integer> selectedIds = new ArrayList<>();

        if (visibleIds.size() >= count) {
            Collections.shuffle(visibleIds);
            selectedIds.addAll(visibleIds.subList(0, count));
        } else {
            for (int i = 0; i < count; i++) {
                int randomIndex = (int)(Math.random() * visibleIds.size());
                selectedIds.add(visibleIds.get(randomIndex));
            }
        }

        List<Advertisement> result = selectedIds.stream()
            .map(advertisementRepository::getById)
            .toList();

        return AdvertisementsResponse.from(result);
    }

    @Transactional
    public void increaseClickCount(Integer id) {
        int updatedCount = advertisementRepository.incrementClickCount(id);
        if (updatedCount == 0) {
            throw CustomException.of(NOT_FOUND_ADVERTISEMENT);
        }
    }
}
