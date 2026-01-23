package gg.agit.konect.admin.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

import gg.agit.konect.admin.schedule.dto.AdminScheduleCreateRequest;
import gg.agit.konect.admin.schedule.dto.AdminScheduleUpsertRequest;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.auth.annotation.Auth;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Admin) Schedule: 일정", description = "어드민 일정 API")
@RequestMapping("/admin/schedule")
public interface AdminScheduleApi {

    @Operation(summary = "일정을 생성한다.", description = """
        **scheduleType (일정 구분):**
        - `UNIVERSITY`: 대학교 일정
        - `CLUB`: 동아리 일정
        - `COUNCIL`: 총동아리연합회 일정
        - `DORM`: 기숙사 일정
        """)
    @PostMapping
    @Auth(roles = {UserRole.ADMIN})
    ResponseEntity<Void> createSchedule(
        @Valid @RequestBody AdminScheduleCreateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "일정을 일괄 생성/수정한다.", description = """
        scheduleId가 없으면 신규 생성, 있으면 해당 일정 수정입니다.

        **scheduleType (일정 구분):**
        - `UNIVERSITY`: 대학교 일정
        - `CLUB`: 동아리 일정
        - `COUNCIL`: 총동아리연합회 일정
        - `DORM`: 기숙사 일정
        """)
    @PutMapping("/batch")
    @Auth(roles = {UserRole.ADMIN})
    ResponseEntity<Void> upsertSchedules(
        @Valid @RequestBody AdminScheduleUpsertRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "일정을 삭제한다.")
    @DeleteMapping("/{scheduleId}")
    @Auth(roles = {UserRole.ADMIN})
    ResponseEntity<Void> deleteSchedule(
        @PathVariable Integer scheduleId,
        @UserId Integer userId
    );
}
