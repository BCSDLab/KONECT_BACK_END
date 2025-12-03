package gg.agit.konect.notice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.repository.CouncilRepository;
import gg.agit.konect.notice.dto.CouncilNoticesResponse;
import gg.agit.konect.notice.dto.NoticeCreateRequest;
import gg.agit.konect.notice.dto.NoticeResponse;
import gg.agit.konect.notice.model.CouncilNotice;
import gg.agit.konect.notice.repository.CouncilNoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final CouncilNoticeRepository councilNoticeRepository;
    private final CouncilRepository councilRepository;

    public CouncilNoticesResponse getNotices(Integer page, Integer limit) {
        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CouncilNotice> councilNoticePage = councilNoticeRepository.findAll(pageable);
        return CouncilNoticesResponse.from(councilNoticePage);
    }

    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request) {
        Council council = councilRepository.getById(1);
        CouncilNotice councilNotice = request.toEntity(council);

        councilNoticeRepository.save(councilNotice);
        return NoticeResponse.from(councilNotice);
    }
}
