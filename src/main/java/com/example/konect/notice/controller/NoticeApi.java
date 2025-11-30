package com.example.konect.notice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.konect.notice.dto.CouncilNoticesResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Notice: 공지사항", description = "공지사항 API")
public interface NoticeApi {

    @Operation(summary = "페이지네이션으로 공지사항을 조회한다.")
    @GetMapping("/councils/notices")
    ResponseEntity<CouncilNoticesResponse> getNotices(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit
    );
}
