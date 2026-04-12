package gg.agit.konect.domain.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.event.dto.EventBoothMapResponse;
import gg.agit.konect.domain.event.dto.EventBoothsResponse;
import gg.agit.konect.domain.event.dto.EventContentsResponse;
import gg.agit.konect.domain.event.dto.EventHomeResponse;
import gg.agit.konect.domain.event.dto.EventMiniEventsResponse;
import gg.agit.konect.domain.event.dto.EventProgramsResponse;
import gg.agit.konect.domain.event.enums.EventProgramType;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@Tag(name = "(Normal) Event: 행사", description = "행사 API")
@RequestMapping("/events")
public interface EventApi {

    @Operation(summary = "행사 홈 정보를 조회한다.")
    @GetMapping("/{eventId}/home")
    ResponseEntity<EventHomeResponse> getEventHome(
        @PathVariable Integer eventId,
        @UserId Integer userId
    );

    @Operation(summary = "행사 프로그램 목록을 조회한다.")
    @GetMapping("/{eventId}/programs")
    ResponseEntity<EventProgramsResponse> getEventPrograms(
        @PathVariable Integer eventId,
        @RequestParam(defaultValue = "ALL") EventProgramType type,
        @RequestParam(defaultValue = "1") @Min(1) Integer page,
        @RequestParam(defaultValue = "20") @Min(1) Integer limit,
        @UserId Integer userId
    );

    @Operation(summary = "행사 부스 목록을 조회한다.")
    @GetMapping("/{eventId}/booths")
    ResponseEntity<EventBoothsResponse> getEventBooths(
        @PathVariable Integer eventId,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") @Min(1) Integer page,
        @RequestParam(defaultValue = "20") @Min(1) Integer limit
    );

    @Operation(summary = "행사 부스 맵을 조회한다.")
    @GetMapping("/{eventId}/booth-map")
    ResponseEntity<EventBoothMapResponse> getEventBoothMap(
        @PathVariable Integer eventId
    );

    @Operation(summary = "행사 미니 이벤트 목록을 조회한다.")
    @GetMapping("/{eventId}/mini-events")
    ResponseEntity<EventMiniEventsResponse> getEventMiniEvents(
        @PathVariable Integer eventId,
        @RequestParam(defaultValue = "1") @Min(1) Integer page,
        @RequestParam(defaultValue = "20") @Min(1) Integer limit,
        @UserId Integer userId
    );

    @Operation(summary = "행사 콘텐츠 목록을 조회한다.")
    @GetMapping("/{eventId}/contents")
    ResponseEntity<EventContentsResponse> getEventContents(
        @PathVariable Integer eventId,
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "1") @Min(1) Integer page,
        @RequestParam(defaultValue = "20") @Min(1) Integer limit
    );
}
