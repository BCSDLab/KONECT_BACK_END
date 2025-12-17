package gg.agit.konect.domain.schedule.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.schedule.dto.UniversitySchedulesResponse;
import gg.agit.konect.domain.schedule.dto.UniversitySchedulesResponse.InnerUniversityScheduleResponse;
import gg.agit.konect.domain.schedule.model.UniversitySchedule;
import gg.agit.konect.domain.schedule.repository.UniversityScheduleRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityScheduleService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UniversityScheduleRepository universityScheduleRepository;
    private final UserRepository userRepository;

    public UniversitySchedulesResponse getUniversitySchedules(Integer userId) {
        User user = userRepository.getById(userId);
        LocalDate today = LocalDate.now();
        List<UniversitySchedule> schedules = universityScheduleRepository
            .findByUniversityIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(
                user.getUniversity().getId(),
                today
            );

        List<InnerUniversityScheduleResponse> responses = schedules.stream()
            .map(this::toResponse)
            .toList();

        return new UniversitySchedulesResponse(responses);
    }

    private InnerUniversityScheduleResponse toResponse(UniversitySchedule schedule) {
        String time = formatTime(schedule);
        Integer dDay = calculateDDay(schedule.getStartedAt());

        return new InnerUniversityScheduleResponse(
            schedule.getTitle(),
            schedule.getStartedAt(),
            time,
            dDay
        );
    }

    private String formatTime(UniversitySchedule schedule) {
        if (schedule.getStartTime() == null && schedule.getEndTime() == null) {
            return null;
        }

        if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
            return schedule.getStartTime().format(TIME_FORMATTER) +
                " ~ " + schedule.getEndTime().format(TIME_FORMATTER);
        }

        if (schedule.getStartTime() != null) {
            return schedule.getStartTime().format(TIME_FORMATTER);
        }

        return schedule.getEndTime().format(TIME_FORMATTER);
    }

    private Integer calculateDDay(LocalDate startedAt) {
        LocalDate today = LocalDate.now();
        return (int) ChronoUnit.DAYS.between(today, startedAt);
    }
}
