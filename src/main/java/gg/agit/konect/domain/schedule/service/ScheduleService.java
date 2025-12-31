package gg.agit.konect.domain.schedule.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.schedule.dto.SchedulesResponse;
import gg.agit.konect.domain.schedule.model.UniversitySchedule;
import gg.agit.konect.domain.schedule.repository.UniversityScheduleRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private static final int UPCOMING_SCHEDULES_LIMIT = 3;

    private final UniversityScheduleRepository universityScheduleRepository;
    private final UserRepository userRepository;

    public SchedulesResponse getUpcomingSchedules(Integer userId) {
        User user = userRepository.getById(userId);
        List<UniversitySchedule> universitySchedules = universityScheduleRepository.findUpcomingSchedulesWithLimit(
            user.getUniversity().getId(),
            LocalDate.now().atStartOfDay(),
            PageRequest.of(0, UPCOMING_SCHEDULES_LIMIT)
        );

        return SchedulesResponse.from(universitySchedules);
    }

    public SchedulesResponse getSchedules(Integer userId) {
        User user = userRepository.getById(userId);
        List<UniversitySchedule> universitySchedules = universityScheduleRepository.findUpcomingSchedules(
            user.getUniversity().getId(),
            LocalDate.now().atStartOfDay()
        );

        return SchedulesResponse.from(universitySchedules);
    }
}
