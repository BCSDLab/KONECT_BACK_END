package gg.agit.konect.admin.advertisement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementCreateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementResponse;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementUpdateRequest;
import gg.agit.konect.admin.advertisement.dto.AdminAdvertisementsResponse;
import gg.agit.konect.domain.advertisement.model.Advertisement;
import gg.agit.konect.domain.advertisement.repository.AdvertisementRepository;

@Service
@Transactional(readOnly = true)
public class AdminAdvertisementService {

    private final AdvertisementRepository advertisementRepository;

    public AdminAdvertisementService(AdvertisementRepository advertisementRepository) {
        this.advertisementRepository = advertisementRepository;
    }

    public AdminAdvertisementsResponse getAdvertisements() {
        return AdminAdvertisementsResponse.from(advertisementRepository.findAllByOrderByCreatedAtDesc());
    }

    public AdminAdvertisementResponse getAdvertisement(Integer id) {
        Advertisement advertisement = advertisementRepository.getById(id);
        return AdminAdvertisementResponse.from(advertisement);
    }

    @Transactional
    public void createAdvertisement(AdminAdvertisementCreateRequest request) {
        Advertisement advertisement = Advertisement.of(
                request.title(),
                request.description(),
                request.imageUrl(),
                request.linkUrl(),
                request.isVisible()
        );
        advertisementRepository.save(advertisement);
    }

    @Transactional
    public void updateAdvertisement(Integer id, AdminAdvertisementUpdateRequest request) {
        Advertisement advertisement = advertisementRepository.getById(id);
        advertisement.update(
                request.title(),
                request.description(),
                request.imageUrl(),
                request.linkUrl(),
                request.isVisible()
        );
    }

    @Transactional
    public void deleteAdvertisement(Integer id) {
        Advertisement advertisement = advertisementRepository.getById(id);
        advertisementRepository.delete(advertisement);
    }
}
