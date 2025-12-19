package gg.agit.konect.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import gg.agit.konect.domain.schedule.dto.SchedulesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;

@Tag(name = "(Normal) Schedule: 일정", description = "일정 API")
public interface ScheduleApi {

    @Operation(summary = "대학교 일정을 조회한다.", description = """
        오늘을 기준으로 종료일이 오늘 이후인 일정들을 조회합니다.

        **dDay 계산 규칙:**
        - 오늘이 일정 **시작 전**인 경우: D-Day 계산 (시작일까지 남은 일수)
        - 오늘이 일정 **당일 또는 진행중**인 경우: null

        ### 예시 1) 여러 날 일정 (12.15 ~ 12.17)
        - 오늘이 12.13 → dDay: 2 (시작까지 2일 남음)
        - 오늘이 12.15 → dDay: null (당일 시작)
        - 오늘이 12.16 → dDay: null (진행중)

        **예시 2) 하루 일정 (12.15)**
        - 오늘이 12.13 → dDay: 2 (시작까지 2일 남음)
        - 오늘이 12.15 → dDay: null (당일)
        
        **startedAt, endedAt**
        - 시간이 정해지지 않는 경우 : 00:00
        - 시간이 정해진 경우 : 정해진 시간
        """)
    @GetMapping("/schedules")
    ResponseEntity<SchedulesResponse> getSchedules(HttpSession session);
}
