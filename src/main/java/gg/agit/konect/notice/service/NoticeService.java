package gg.agit.konect.notice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.repository.CouncilRepository;
import gg.agit.konect.notice.dto.CouncilNoticesResponse;
import gg.agit.konect.notice.dto.CouncilNoticeCreateRequest;
import gg.agit.konect.notice.dto.CouncilNoticeResponse;
import gg.agit.konect.notice.dto.CouncilNoticeUpdateRequest;
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

    public CouncilNoticeResponse getNotice(Integer id) {
        CouncilNotice councilNotice = councilNoticeRepository.getById(id);
        return CouncilNoticeResponse.from(councilNotice);
    }

    @Transactional
    public void createNotice(CouncilNoticeCreateRequest request) {
        Council council = councilRepository.getById(1);
        CouncilNotice councilNotice = request.toEntity(council);

        councilNoticeRepository.save(councilNotice);
    }

    @Transactional
    public void updateNotice(Integer id, CouncilNoticeUpdateRequest request) {
        CouncilNotice councilNotice = councilNoticeRepository.getById(id);
        councilNotice.update(request.title(), request.content());
    }

    @Transactional
    public void deleteNotice(Integer id) {
        CouncilNotice councilNotice = councilNoticeRepository.getById(id);
        councilNoticeRepository.deleteById(councilNotice.getId());
    }
}
