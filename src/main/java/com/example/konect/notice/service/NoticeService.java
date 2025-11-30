package com.example.konect.notice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.konect.notice.dto.CouncilNoticesResponse;
import com.example.konect.notice.model.CouncilNotice;
import com.example.konect.notice.repository.CouncilNoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final CouncilNoticeRepository councilNoticeRepository;

    public CouncilNoticesResponse getNotices(Integer page, Integer limit) {
        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CouncilNotice> councilNoticePage = councilNoticeRepository.findAll(pageable);
        return CouncilNoticesResponse.from(councilNoticePage);
    }
}
