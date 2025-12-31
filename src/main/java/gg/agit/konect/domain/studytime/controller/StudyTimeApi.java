package gg.agit.konect.domain.studytime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Study Time: 순공 시간", description = "순공 시간 API")
@RequestMapping("/study-times")
public interface StudyTimeApi {

    @Operation(summary = "스터디 타이머를 시작한다.", description = """
        ## 설명
        - 스터디 타이머를 시작합니다.
        - 사용자당 동시에 1개의 타이머만 허용됩니다.

        ## 에러
        - `ALREADY_RUNNING_STUDY_TIMER` (409): 이미 실행 중인 타이머가 있는 경우
        """)
    @PostMapping("/start")
    ResponseEntity<Void> start(@UserId Integer userId);

    @Operation(summary = "스터디 타이머를 종료한다.", description = """
        ## 설명
        - 실행 중인 타이머를 종료하고 공부 시간을 집계합니다.
        - 시간이 자정을 넘기면 날짜별로 분할 집계합니다.
        - 일간, 월간, 총 누적 시간을 함께 갱신합니다.

        ## 에러
        - `STUDY_TIMER_TIME_MISMATCH` (400): 클라이언트 누적 시간과 서버 시간 차이가 1분 이상인 경우
        - `STUDY_TIMER_NOT_RUNNING` (400): 실행 중인 타이머가 없는 경우
        """)
    @PostMapping("/stop")
    ResponseEntity<StudyTimerStopResponse> stop(
        @UserId Integer userId,
        @RequestBody @Valid StudyTimerStopRequest request
    );
}
