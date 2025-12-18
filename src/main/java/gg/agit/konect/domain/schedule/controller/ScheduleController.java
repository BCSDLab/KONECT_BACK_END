package gg.agit.konect.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.schedule.dto.UniversitySchedulesResponse;
import gg.agit.konect.domain.schedule.service.UniversityScheduleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApi {

    private final UniversityScheduleService universityScheduleService;

    @GetMapping("/schedules/universities")
    public ResponseEntity<UniversitySchedulesResponse> getUniversitySchedules(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        UniversitySchedulesResponse response = universityScheduleService.getUniversitySchedules(userId);
        return ResponseEntity.ok(response);
    }
}
