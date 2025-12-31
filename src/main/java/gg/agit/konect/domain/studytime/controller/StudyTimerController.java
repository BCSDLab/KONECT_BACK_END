package gg.agit.konect.domain.studytime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.domain.studytime.model.StudyTimeAggregate;
import gg.agit.konect.domain.studytime.service.StudyTimerService;
import gg.agit.konect.global.auth.annotation.UserId;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/study-timers")
public class StudyTimerController implements StudyTimerApi {

    private final StudyTimerService studyTimerService;

    @PostMapping("/start")
    public ResponseEntity<Void> start(@UserId Integer userId) {
        studyTimerService.start(userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/stop")
    public ResponseEntity<StudyTimerStopResponse> stop(@UserId Integer userId) {
        StudyTimeAggregate aggregate = studyTimerService.stop(userId);

        return ResponseEntity.ok(StudyTimerStopResponse.of(
            aggregate.sessionSeconds(),
            aggregate.dailySeconds(),
            aggregate.monthlySeconds(),
            aggregate.totalSeconds()
        ));
    }
}
