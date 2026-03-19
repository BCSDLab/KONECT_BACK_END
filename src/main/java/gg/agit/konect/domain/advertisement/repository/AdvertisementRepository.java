package gg.agit.konect.domain.advertisement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.advertisement.model.Advertisement;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface AdvertisementRepository extends Repository<Advertisement, Integer> {

    Advertisement save(Advertisement advertisement);

    Optional<Advertisement> findById(Integer id);

    List<Advertisement> findAllByOrderByCreatedAtDesc();

    List<Advertisement> findAllByIsVisibleTrueOrderByCreatedAtDesc();

    void delete(Advertisement advertisement);

    default Advertisement getById(Integer id) {
        return findById(id)
                .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_ADVERTISEMENT));
    }
}
