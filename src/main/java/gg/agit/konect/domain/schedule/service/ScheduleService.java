package gg.agit.konect.domain.schedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.schedule.dto.ScheduleCondition;
import gg.agit.konect.domain.schedule.dto.SchedulesResponse;
import gg.agit.konect.domain.schedule.model.Schedule;
import gg.agit.konect.domain.schedule.repository.ScheduleRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private static final int UPCOMING_SCHEDULES_LIMIT = 3;

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public SchedulesResponse getUpcomingSchedules(Integer userId) {
        User user = userRepository.getById(userId);
        List<Schedule> schedules = scheduleRepository.findUpcomingSchedules(
            user.getUniversity().getId(),
            LocalDate.now().atStartOfDay(),
            PageRequest.of(0, UPCOMING_SCHEDULES_LIMIT)
        );

        return SchedulesResponse.from(schedules);
    }

    public SchedulesResponse getSchedules(ScheduleCondition condition, Integer userId) {
        User user = userRepository.getById(userId);

        YearMonth yearMonth = YearMonth.of(condition.year(), condition.month());
        LocalDateTime monthStartAt = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEndAt = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        List<Schedule> schedules = scheduleRepository.findSchedulesByMonth(
            user.getUniversity().getId(),
            monthStartAt,
            monthEndAt
        );

        return SchedulesResponse.from(schedules);
    }
}
