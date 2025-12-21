package gg.agit.konect.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.schedule.dto.SchedulesResponse;
import gg.agit.konect.domain.schedule.service.ScheduleService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApi {

    private final ScheduleService scheduleService;

    @GetMapping("/schedules")
    public ResponseEntity<SchedulesResponse> getSchedules(@UserId Integer userId) {
        SchedulesResponse response = scheduleService.getSchedules(userId);
        return ResponseEntity.ok(response);
    }
}
