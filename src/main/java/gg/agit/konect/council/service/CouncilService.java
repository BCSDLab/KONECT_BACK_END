package gg.agit.konect.council.service;

import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.council.dto.CouncilResponse;
import gg.agit.konect.council.dto.CouncilUpdateRequest;
import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.model.CouncilOperatingHour;
import gg.agit.konect.council.model.CouncilSocialMedia;
import gg.agit.konect.council.repository.CouncilOperatingHourRepository;
import gg.agit.konect.council.repository.CouncilRepository;
import gg.agit.konect.council.repository.CouncilSocialMediaRepository;
import gg.agit.konect.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouncilService {

    private final CouncilRepository councilRepository;
    private final CouncilOperatingHourRepository councilOperatingHourRepository;
    private final CouncilSocialMediaRepository councilSocialMediaRepository;
    private final EntityManager entityManager;

    public CouncilResponse getCouncil() {
        Council council = councilRepository.getById(1);
        List<CouncilOperatingHour> operatingHours = councilOperatingHourRepository.findByCouncilId(council.getId());
        List<CouncilSocialMedia> socialMedias = councilSocialMediaRepository.findByCouncilId(council.getId());

        return CouncilResponse.of(council, operatingHours, socialMedias);
    }

    @Transactional
    public CouncilResponse updateCouncil(CouncilUpdateRequest request) {
        validateOperatingHours(request.operatingHours());

        Council council = councilRepository.getById(1);
        council.update(
            request.name(),
            request.introduce(),
            request.location(),
            request.phoneNumber(),
            request.email()
        );

        councilOperatingHourRepository.deleteByCouncilId(council.getId());
        councilSocialMediaRepository.deleteByCouncilId(council.getId());
        entityManager.flush();

        List<CouncilOperatingHour> operatingHours = request.operatingHours().stream()
            .map(operatingHour -> operatingHour.toEntity(council))
            .toList();
        operatingHours.forEach(councilOperatingHourRepository::save);

        List<CouncilSocialMedia> socialMedias = request.socialMedias().stream()
            .map(socialMedia -> socialMedia.toEntity(council))
            .toList();
        socialMedias.forEach(councilSocialMediaRepository::save);

        return CouncilResponse.of(council, operatingHours, socialMedias);
    }

    private void validateOperatingHours(List<CouncilUpdateRequest.InnerOperatingHour> operatingHours) {
        validateAllDaysPresent(operatingHours);
        operatingHours.forEach(this::validateOperatingHourTimes);
    }

    private void validateAllDaysPresent(List<CouncilUpdateRequest.InnerOperatingHour> operatingHours) {
        Set<DayOfWeek> providedDays = operatingHours.stream()
            .map(CouncilUpdateRequest.InnerOperatingHour::dayOfWeek)
            .collect(Collectors.toSet());
        Set<DayOfWeek> allDays = Arrays.stream(DayOfWeek.values()).collect(Collectors.toSet());

        if (!providedDays.equals(allDays)) {
            throw CustomException.of(INVALID_OPERATING_HOURS_DAYS);
        }
    }

    private void validateOperatingHourTimes(CouncilUpdateRequest.InnerOperatingHour operatingHour) {
        if (operatingHour.isClosed()) {
            if (operatingHour.openTime() != null || operatingHour.closeTime() != null) {
                throw CustomException.of(INVALID_OPERATING_HOURS_CLOSED);
            }
        } else {
            if (operatingHour.openTime() == null || operatingHour.closeTime() == null) {
                throw CustomException.of(INVALID_OPERATING_HOURS_TIME);
            }
            if (!operatingHour.openTime().isBefore(operatingHour.closeTime())) {
                throw CustomException.of(INVALID_OPERATING_HOURS_TIME);
            }
        }
    }
}