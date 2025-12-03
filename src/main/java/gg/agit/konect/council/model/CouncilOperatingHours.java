package gg.agit.konect.council.model;

import static gg.agit.konect.global.code.ApiResponseCode.INVALID_OPERATING_HOURS_DAYS;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import gg.agit.konect.global.exception.CustomException;

public record CouncilOperatingHours(
    List<CouncilOperatingHour> operatingHours
) {

    public CouncilOperatingHours {
        validateOperatingHours(operatingHours);
    }

    private void validateOperatingHours(List<CouncilOperatingHour> operatingHours) {
        validateAllDaysPresent(operatingHours);
        operatingHours.forEach(CouncilOperatingHour::validate);
    }

    private void validateAllDaysPresent(List<CouncilOperatingHour> operatingHours) {
        Set<DayOfWeek> providedDays = operatingHours.stream()
            .map(CouncilOperatingHour::getDayOfWeek)
            .collect(Collectors.toSet());
        Set<DayOfWeek> allDays = Arrays.stream(DayOfWeek.values()).collect(Collectors.toSet());

        if (!providedDays.equals(allDays)) {
            throw CustomException.of(INVALID_OPERATING_HOURS_DAYS);
        }
    }
}
