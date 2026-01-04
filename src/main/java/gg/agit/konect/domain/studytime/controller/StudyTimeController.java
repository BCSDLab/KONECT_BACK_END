package gg.agit.konect.domain.studytime.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.studytime.dto.StudyTimeRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingsResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimeSummaryResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.domain.studytime.service.StudyTimeQueryService;
import gg.agit.konect.domain.studytime.service.StudyTimeRankingService;
import gg.agit.konect.domain.studytime.service.StudyTimerService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studytimes")
public class StudyTimeController implements StudyTimeApi {

    private final StudyTimerService studyTimerService;
    private final StudyTimeQueryService studyTimeQueryService;
    private final StudyTimeRankingService studyTimeRankingService;

    @Override
    public ResponseEntity<StudyTimeSummaryResponse> getSummary(@UserId Integer userId) {
        StudyTimeSummaryResponse response = studyTimeQueryService.getSummary(userId);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<StudyTimeRankingsResponse> getRankings(
        @Valid @ParameterObject @ModelAttribute StudyTimeRankingCondition condition,
        @UserId Integer userId
    ) {
        StudyTimeRankingsResponse response = studyTimeRankingService.getRankings(condition);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> start(@UserId Integer userId) {
        studyTimerService.start(userId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<StudyTimerStopResponse> stop(
        @UserId Integer userId,
        @RequestBody @Valid StudyTimerStopRequest request
    ) {
        StudyTimerStopResponse response = studyTimerService.stop(userId, request);

        return ResponseEntity.ok(response);
    }
}
