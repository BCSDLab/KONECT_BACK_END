package gg.agit.konect.domain.studytime.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.studytime.dto.StudyTimeRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingsResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimeSummaryResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopRequest;
import gg.agit.konect.domain.studytime.dto.StudyTimerStopResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Study Time: 순공 시간", description = "순공 시간 API")
@RequestMapping("/studytimes")
public interface StudyTimeApi {

    @Operation(summary = "순공 시간(일간, 월간, 통합)을 조회한다.")
    @GetMapping("/summary")
    ResponseEntity<StudyTimeSummaryResponse> getSummary(@UserId Integer userId);

    @Operation(summary = "공부 시간 랭킹을 조회한다.", description = """
        ## 설명
        - 페이지네이션으로 랭킹을 조회합니다.
        - sort는 월간(MONTHLY) 또는 일간(DAILY) 기준으로 정렬됩니다.
        - 시간이 같은 경우 다른 기간의 시간을 기준으로 추가 정렬됩니다.
        - 그 마저 같다면 id를 기준으로 정렬됩니다.
        - name은 type에 따라 동아리명/학번(두 자리 숫자)/개인 이름으로 반환됩니다.

        ## type (랭킹 기준)
        - `CLUB`: 동아리 랭킹
        - `STUDENT_NUMBER`: 학번 랭킹
        - `PERSONAL`: 개인 랭킹

        ## sort (정렬 기준)
        - `MONTHLY`: 이번 달 기준 정렬
        - `DAILY`: 오늘 기준 정렬
        """)
    @GetMapping("/rankings")
    ResponseEntity<StudyTimeRankingsResponse> getRankings(
        @Valid @ParameterObject @ModelAttribute StudyTimeRankingCondition condition,
        @UserId Integer userId
    );

    @Operation(summary = "스터디 타이머를 시작한다.", description = """
        ## 설명
        - 스터디 타이머를 시작합니다.
        - 사용자당 동시에 1개의 타이머만 허용됩니다.

        ## 에러
        - `ALREADY_RUNNING_STUDY_TIMER` (409): 이미 실행 중인 타이머가 있는 경우
        """)
    @PostMapping("/timers")
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
    @DeleteMapping("/timers")
    ResponseEntity<StudyTimerStopResponse> stop(
        @UserId Integer userId,
        @RequestBody @Valid StudyTimerStopRequest request
    );
}
