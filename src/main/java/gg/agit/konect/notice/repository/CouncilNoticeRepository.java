package gg.agit.konect.notice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import gg.agit.konect.notice.model.CouncilNotice;

public interface CouncilNoticeRepository extends Repository<CouncilNotice, Integer> {

    Page<CouncilNotice> findAll(Pageable pageable);

    void save(CouncilNotice councilNotice);
}
