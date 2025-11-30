package com.example.konect.notice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.konect.notice.dto.NoticesResponse;
import com.example.konect.notice.service.NoticeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NoticeController implements NoticeApi {

    private final NoticeService noticeService;

    @GetMapping("/councils/notices")
    public ResponseEntity<NoticesResponse> getNotices(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit
    ) {
        NoticesResponse response = noticeService.getNotices(page, limit);
        return ResponseEntity.ok(response);
    }
}
