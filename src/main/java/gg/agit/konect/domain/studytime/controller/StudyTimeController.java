package gg.agit.konect.domain.studytime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.domain.studytime.service.StudyTimerService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/study-times")
public class StudyTimeController implements StudyTimeApi {

    private final StudyTimerService studyTimerService;

    @PostMapping("/start")
    public ResponseEntity<Void> start(@UserId Integer userId) {
        studyTimerService.start(userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/stop")
    public ResponseEntity<StudyTimerStopResponse> stop(
        @UserId Integer userId,
        @RequestBody @Valid StudyTimerStopRequest request
    ) {
        StudyTimerStopResponse response = studyTimerService.stop(userId, request);

        return ResponseEntity.ok(response);
    }
}
