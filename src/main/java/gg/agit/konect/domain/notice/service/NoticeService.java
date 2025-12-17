package gg.agit.konect.domain.notice.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.council.model.Council;
import gg.agit.konect.domain.council.repository.CouncilRepository;
import gg.agit.konect.domain.notice.dto.CouncilNoticeCreateRequest;
import gg.agit.konect.domain.notice.dto.CouncilNoticeResponse;
import gg.agit.konect.domain.notice.dto.CouncilNoticeUpdateRequest;
import gg.agit.konect.domain.notice.dto.CouncilNoticesResponse;
import gg.agit.konect.domain.notice.model.CouncilNotice;
import gg.agit.konect.domain.notice.model.CouncilNoticeReadHistory;
import gg.agit.konect.domain.notice.repository.CouncilNoticeReadRepository;
import gg.agit.konect.domain.notice.repository.CouncilNoticeRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final CouncilNoticeReadRepository councilNoticeReadRepository;
    private final CouncilNoticeRepository councilNoticeRepository;
    private final CouncilRepository councilRepository;
    private final UserRepository userRepository;

    public CouncilNoticesResponse getNotices(Integer page, Integer limit, Integer userId) {
        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CouncilNotice> councilNoticePage = councilNoticeRepository.findAll(pageable);
        Map<Integer, Boolean> councilNoticeReadMap = getCouncilNoticeReadMap(userId, councilNoticePage.getContent());
        return CouncilNoticesResponse.from(councilNoticePage, councilNoticeReadMap);
    }

    private Map<Integer, Boolean> getCouncilNoticeReadMap(Integer userId, List<CouncilNotice> councilNotices) {
        User user = userRepository.getById(userId);
        Set<Integer> readNoticeIds = getReadNoticeIds(user.getId(), councilNotices);

        return councilNotices.stream()
            .collect(Collectors.toMap(
                CouncilNotice::getId,
                notice -> readNoticeIds.contains(notice.getId())
            ));
    }

    private Set<Integer> getReadNoticeIds(Integer userId, List<CouncilNotice> councilNotices) {
        List<Integer> noticeIds = councilNotices.stream()
            .map(CouncilNotice::getId)
            .toList();

        List<CouncilNoticeReadHistory> councilNoticeReadHistories =
            councilNoticeReadRepository.findByUserIdAndCouncilNoticeIdIn(userId, noticeIds);

        return councilNoticeReadHistories.stream()
            .map(history -> history.getCouncilNotice().getId())
            .collect(Collectors.toSet());
    }

    @Transactional
    public CouncilNoticeResponse getNotice(Integer id, Integer userId) {
        CouncilNotice councilNotice = councilNoticeRepository.getById(id);
        User user = userRepository.getById(userId);

        if (!councilNoticeReadRepository.existsByUserIdAndCouncilNoticeId(userId, id)) {
            councilNoticeReadRepository.save(CouncilNoticeReadHistory.of(user, councilNotice));
        }

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
