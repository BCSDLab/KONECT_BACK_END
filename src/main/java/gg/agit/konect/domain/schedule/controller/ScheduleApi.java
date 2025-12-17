package gg.agit.konect.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import gg.agit.konect.domain.schedule.dto.UniversitySchedulesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;

@Tag(name = "(Normal) Schedule: 일정", description = "일정 API")
public interface ScheduleApi {

    @Operation(summary = "대학교 일정을 조회한다.")
    @GetMapping("/schedules")
    ResponseEntity<UniversitySchedulesResponse> getUniversitySchedules(HttpSession session);
}
