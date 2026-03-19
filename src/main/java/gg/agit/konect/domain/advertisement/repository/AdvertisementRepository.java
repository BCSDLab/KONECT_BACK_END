package gg.agit.konect.domain.advertisement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.advertisement.model.Advertisement;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface AdvertisementRepository extends Repository<Advertisement, Integer> {

    Advertisement save(Advertisement advertisement);

    Optional<Advertisement> findById(Integer id);

    List<Advertisement> findAllByOrderByCreatedAtDesc();

    List<Advertisement> findAllByIsVisibleTrueOrderByCreatedAtDesc();

    void delete(Advertisement advertisement);

    /**
     * 광고 클릭 수를 원자적으로 증가시킵니다.
     * 동시성 문제를 방지하기 위해 DB 레벨에서 UPDATE ... SET click_count = click_count + 1을 수행합니다.
     */
    @Modifying
    @Query("UPDATE Advertisement a SET a.clickCount = a.clickCount + 1 WHERE a.id = :id")
    void incrementClickCount(@Param("id") Integer id);

    default Advertisement getById(Integer id) {
        return findById(id)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_ADVERTISEMENT));
    }
}
