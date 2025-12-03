package gg.agit.konect.notice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.notice.dto.CouncilNoticesResponse;
import gg.agit.konect.notice.dto.NoticeCreateRequest;
import gg.agit.konect.notice.dto.NoticeResponse;
import gg.agit.konect.notice.dto.NoticeUpdateRequest;
import gg.agit.konect.notice.service.NoticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NoticeController implements NoticeApi {

    private final NoticeService noticeService;

    @GetMapping("/councils/notices")
    public ResponseEntity<CouncilNoticesResponse> getNotices(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit
    ) {
        CouncilNoticesResponse response = noticeService.getNotices(page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/councils/notices/{id}")
    public ResponseEntity<NoticeResponse> getNotice(
        @PathVariable Integer id
    ) {
        NoticeResponse response = noticeService.getNotice(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/councils/notices")
    public ResponseEntity<Void> createNotice(
        @Valid @RequestBody NoticeCreateRequest request
    ) {
        noticeService.createNotice(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/councils/notices/{id}")
    public ResponseEntity<Void> updateNotice(
        @PathVariable Integer id,
        @Valid @RequestBody NoticeUpdateRequest request
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
