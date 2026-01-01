package gg.agit.konect.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.schedule.dto.ScheduleCondition;
import gg.agit.konect.domain.schedule.dto.SchedulesResponse;
import gg.agit.konect.domain.schedule.service.ScheduleService;
import gg.agit.konect.global.auth.annotation.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApi {

    private final ScheduleService scheduleService;

    @Override
    public ResponseEntity<SchedulesResponse> getUpcomingSchedules(
        @UserId Integer userId
    ) {
        SchedulesResponse response = scheduleService.getUpcomingSchedules(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SchedulesResponse> getSchedules(
        @Valid @ModelAttribute ScheduleCondition condition,
        @UserId Integer userId
    ) {
        SchedulesResponse response = scheduleService.getSchedules(condition, userId);
        return ResponseEntity.ok(response);
    }
}
