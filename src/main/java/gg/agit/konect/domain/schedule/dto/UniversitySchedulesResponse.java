package gg.agit.konect.domain.schedule.dto;

import java.time.LocalDate;
import java.util.List;

public record UniversitySchedulesResponse(
    List<InnerUniversityScheduleResponse> universitySchedules
) {
    public record InnerUniversityScheduleResponse(
        String title,
        LocalDate startedAt,
        String time,
        Integer dDay
    ) {

    }
}
