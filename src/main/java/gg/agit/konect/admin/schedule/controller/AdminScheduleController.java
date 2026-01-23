package gg.agit.konect.admin.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.admin.schedule.dto.AdminScheduleCreateRequest;
import gg.agit.konect.admin.schedule.dto.AdminScheduleUpsertRequest;
import gg.agit.konect.admin.schedule.service.AdminScheduleService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/schedule")
public class AdminScheduleController implements AdminScheduleApi {

    private final AdminScheduleService adminScheduleService;

    @Override
    public ResponseEntity<Void> createSchedule(AdminScheduleCreateRequest request, Integer userId) {
        adminScheduleService.createSchedule(request, userId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> upsertSchedules(AdminScheduleUpsertRequest request, Integer userId) {
        adminScheduleService.upsertSchedules(request, userId);

        return ResponseEntity.ok().build();
    }
}
