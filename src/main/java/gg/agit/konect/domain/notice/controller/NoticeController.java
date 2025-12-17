package gg.agit.konect.domain.notice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.notice.dto.CouncilNoticesResponse;
import gg.agit.konect.domain.notice.dto.CouncilNoticeCreateRequest;
import gg.agit.konect.domain.notice.dto.CouncilNoticeResponse;
import gg.agit.konect.domain.notice.dto.CouncilNoticeUpdateRequest;
import gg.agit.konect.domain.notice.service.NoticeService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NoticeController implements NoticeApi {

    private final NoticeService noticeService;

    @GetMapping("/councils/notices")
    public ResponseEntity<CouncilNoticesResponse> getNotices(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit,
        HttpSession session
    ) {
        Integer userId = (Integer) session.getAttribute("userId");
        CouncilNoticesResponse response = noticeService.getNotices(page, limit, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/councils/notices/{id}")
    public ResponseEntity<CouncilNoticeResponse> getNotice(
        @PathVariable Integer id,
        HttpSession session
    ) {
        Integer userId = (Integer)session.getAttribute("userId");
        CouncilNoticeResponse response = noticeService.getNotice(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/councils/notices")
    public ResponseEntity<Void> createNotice(
        @Valid @RequestBody CouncilNoticeCreateRequest request
    ) {
        noticeService.createNotice(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/councils/notices/{id}")
    public ResponseEntity<Void> updateNotice(
        @PathVariable Integer id,
        @Valid @RequestBody CouncilNoticeUpdateRequest request
    ) {
        noticeService.updateNotice(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/councils/notices/{id}")
    public ResponseEntity<Void> deleteNotice(
        @PathVariable Integer id
    ) {
        noticeService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }
}
