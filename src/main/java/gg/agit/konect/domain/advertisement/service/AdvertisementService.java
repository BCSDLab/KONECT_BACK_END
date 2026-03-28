package gg.agit.konect.domain.advertisement.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_ADVERTISEMENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
        List<Advertisement> visibleAdvertisements = advertisementRepository.findAllByIsVisibleTrueOrderByCreatedAtDesc();

        if (visibleAdvertisements.isEmpty()) {
            return AdvertisementsResponse.from(List.of());
        }

        List<Advertisement> selectedAdvertisements = new ArrayList<>();

        if (visibleAdvertisements.size() >= count) {
            List<Advertisement> shuffledAdvertisements = new ArrayList<>(visibleAdvertisements);
            Collections.shuffle(shuffledAdvertisements);
            selectedAdvertisements.addAll(shuffledAdvertisements.subList(0, count));
        } else {
            for (int i = 0; i < count; i++) {
                // 등록된 노출 광고 수보다 많은 개수를 요청하면 기존 정책대로 중복 선택을 허용한다.
                int randomIndex = ThreadLocalRandom.current().nextInt(visibleAdvertisements.size());
                selectedAdvertisements.add(visibleAdvertisements.get(randomIndex));
            }
        }

        return AdvertisementsResponse.from(selectedAdvertisements);
    }

    @Transactional
    public void increaseClickCount(Integer id) {
        int updatedCount = advertisementRepository.incrementClickCount(id);
        if (updatedCount == 0) {
            throw CustomException.of(NOT_FOUND_ADVERTISEMENT);
        }
    }
}
