package gg.agit.konect.domain.schedule.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.schedule.dto.UniversitySchedulesResponse;
import gg.agit.konect.domain.schedule.model.UniversitySchedule;
import gg.agit.konect.domain.schedule.repository.UniversityScheduleRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityScheduleService {

    private final UniversityScheduleRepository universityScheduleRepository;
    private final UserRepository userRepository;

    public UniversitySchedulesResponse getUniversitySchedules(Integer userId) {
        User user = userRepository.getById(userId);
        List<UniversitySchedule> schedules = universityScheduleRepository.findUpcomingSchedules(
            user.getUniversity().getId(),
            LocalDate.now()
        );

        return UniversitySchedulesResponse.from(schedules);
    }
}
