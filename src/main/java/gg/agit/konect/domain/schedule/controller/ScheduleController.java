package gg.agit.konect.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.schedule.dto.SchedulesResponse;
import gg.agit.konect.domain.schedule.service.UniversityScheduleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApi {

    private final UniversityScheduleService universityScheduleService;

    @GetMapping("/schedules")
    public ResponseEntity<SchedulesResponse> getUniversitySchedules(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        SchedulesResponse response = universityScheduleService.getUniversitySchedules(userId);
        return ResponseEntity.ok(response);
    }
}
