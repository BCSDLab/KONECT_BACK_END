package gg.agit.konect.admin.schedule.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.admin.schedule.dto.AdminScheduleCreateRequest;
import gg.agit.konect.admin.schedule.dto.AdminScheduleUpsertItemRequest;
import gg.agit.konect.admin.schedule.dto.AdminScheduleUpsertRequest;
import gg.agit.konect.domain.schedule.model.Schedule;
import gg.agit.konect.domain.schedule.model.UniversitySchedule;
import gg.agit.konect.domain.schedule.repository.ScheduleRepository;
import gg.agit.konect.domain.schedule.repository.UniversityScheduleRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminScheduleService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final UniversityScheduleRepository universityScheduleRepository;

    @Transactional
    public void createSchedule(AdminScheduleCreateRequest request, Integer userId) {
        User user = userRepository.getById(userId);
        University university = user.getUniversity();

        Schedule schedule = Schedule.of(
            request.title(),
            request.startedAt(),
            request.endedAt(),
            request.scheduleType()
        );

        Schedule savedSchedule = scheduleRepository.save(schedule);

        UniversitySchedule universitySchedule = UniversitySchedule.of(
            savedSchedule,
            university
        );

        universityScheduleRepository.save(universitySchedule);
    }

    @Transactional
    public void upsertSchedules(AdminScheduleUpsertRequest request, Integer userId) {
        User user = userRepository.getById(userId);
        University university = user.getUniversity();

        for (AdminScheduleUpsertItemRequest item : request.schedules()) {
            if (item.scheduleId() == null) {
                createUniversitySchedule(item, university);
                continue;
            }

            UniversitySchedule universitySchedule = universityScheduleRepository.getByIdAndUniversityId(
                item.scheduleId(),
                university.getId()
            );

            universitySchedule.getSchedule().update(
                item.title(),
                item.startedAt(),
                item.endedAt(),
                item.scheduleType()
            );
        }
    }

    private void createUniversitySchedule(AdminScheduleUpsertItemRequest item, University university) {
        Schedule schedule = Schedule.of(
            item.title(),
            item.startedAt(),
            item.endedAt(),
            item.scheduleType()
        );

        Schedule savedSchedule = scheduleRepository.save(schedule);

        UniversitySchedule universitySchedule = UniversitySchedule.of(
            savedSchedule,
            university
        );

        universityScheduleRepository.save(universitySchedule);
    }
}
