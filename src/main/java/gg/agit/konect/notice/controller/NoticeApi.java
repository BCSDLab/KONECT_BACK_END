package gg.agit.konect.notice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.notice.dto.CouncilNoticesResponse;
import gg.agit.konect.notice.dto.NoticeCreateRequest;
import gg.agit.konect.notice.dto.NoticeResponse;
import gg.agit.konect.notice.dto.NoticeUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Notice: 공지사항", description = "공지사항 API")
public interface NoticeApi {

    @Operation(summary = "페이지네이션으로 총동아리연합회 공지사항을 조회한다.")
    @GetMapping("/councils/notices")
    ResponseEntity<CouncilNoticesResponse> getNotices(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit
    );

    @Operation(
        summary = "총동아리연합회 공지사항 단건을 조회한다.",
        description = """
            총동아리연합회 공지사항의 상세 정보를 조회합니다.
            
            - `NOT_FOUND_COUNCIL_NOTICE` (404): 공지사항을 찾을 수 없습니다.
            """
    )
    @GetMapping("/councils/notices/{id}")
    ResponseEntity<NoticeResponse> getNotice(
        @PathVariable Integer id
    );

    @Operation(
        summary = "총동아리연합회 공지사항을 생성한다.",
        description = """
            총동아리연합회 공지사항을 생성합니다.
            
            - `INVALID_REQUEST_BODY` (400): 요청 본문의 형식이 올바르지 않거나 필수 값이 누락된 경우
            - `NOT_FOUND_COUNCIL` (404): 총동아리연합회를 찾을 수 없습니다.
            """
    )
    @PostMapping("/councils/notices")
    ResponseEntity<NoticeResponse> createNotice(
        @Valid @RequestBody NoticeCreateRequest request
    );

    @Operation(
        summary = "총동아리연합회 공지사항을 수정한다.",
        description = """
            총동아리연합회 공지사항을 수정합니다.
            
            - `INVALID_REQUEST_BODY` (400): 요청 본문의 형식이 올바르지 않거나 필수 값이 누락된 경우
            - `NOT_FOUND_COUNCIL_NOTICE` (404): 총동아리연합회 공지사항을 찾을 수 없습니다.
            """
    )
    @PutMapping("/councils/notices/{id}")
    ResponseEntity<NoticeResponse> updateNotice(
        @PathVariable Integer id,
        @Valid @RequestBody NoticeUpdateRequest request
    );
}
