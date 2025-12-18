package gg.agit.konect.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import gg.agit.konect.domain.schedule.dto.UniversitySchedulesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;

@Tag(name = "(Normal) Schedule: 일정", description = "일정 API")
public interface ScheduleApi {

    @Operation(summary = "대학교 일정을 조회한다.", description = """
        - time의 경우 값이 없는 경우 null, 있는 경우 "HH:mm ~ HH:mm" 형식으로 내려갑니다.
        """)
    @GetMapping("/schedules")
    ResponseEntity<UniversitySchedulesResponse> getUniversitySchedules(HttpSession session);
}
