package gg.agit.konect.domain.notice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.notice.dto.CouncilNoticeCreateRequest;
import gg.agit.konect.domain.notice.dto.CouncilNoticeResponse;
import gg.agit.konect.domain.notice.dto.CouncilNoticeUpdateRequest;
import gg.agit.konect.domain.notice.dto.CouncilNoticesResponse;
import gg.agit.konect.domain.notice.service.NoticeService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
public class NoticeController implements NoticeApi {

    private final NoticeService noticeService;

    @Override
    public ResponseEntity<CouncilNoticesResponse> getNotices(
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit,
        @UserId Integer userId
    ) {
        CouncilNoticesResponse response = noticeService.getNotices(page, limit, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CouncilNoticeResponse> getNotice(
        @PathVariable Integer id,
        @UserId Integer userId
    ) {
        CouncilNoticeResponse response = noticeService.getNotice(id, userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> createNotice(
        @Valid @RequestBody CouncilNoticeCreateRequest request
    ) {
        noticeService.createNotice(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateNotice(
        @PathVariable Integer id,
        @Valid @RequestBody CouncilNoticeUpdateRequest request
    ) {
        noticeService.updateNotice(id, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteNotice(
        @PathVariable Integer id
    ) {
        noticeService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }
}
