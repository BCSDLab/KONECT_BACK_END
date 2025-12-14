package gg.agit.konect.domain.notice.repository;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_COUNCIL_NOTICE;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.domain.notice.model.CouncilNotice;

public interface CouncilNoticeRepository extends Repository<CouncilNotice, Integer> {

    Page<CouncilNotice> findAll(Pageable pageable);

    Optional<CouncilNotice> findById(Integer id);

    default CouncilNotice getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(NOT_FOUND_COUNCIL_NOTICE));
    }

    void save(CouncilNotice councilNotice);

    void deleteById(Integer id);
}
