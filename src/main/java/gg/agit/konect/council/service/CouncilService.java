package gg.agit.konect.council.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.council.dto.CouncilCreateRequest;
import gg.agit.konect.council.dto.CouncilResponse;
import gg.agit.konect.council.dto.CouncilUpdateRequest;
import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.model.CouncilOperatingHours;
import gg.agit.konect.council.model.CouncilSocialMedia;
import gg.agit.konect.council.repository.CouncilOperatingHourRepository;
import gg.agit.konect.council.repository.CouncilRepository;
import gg.agit.konect.council.repository.CouncilSocialMediaRepository;
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

    @Transactional
    public void createCouncil(CouncilCreateRequest request) {
        Council council = request.toEntity();
        councilRepository.save(council);
    }

    public CouncilResponse getCouncil() {
        Council council = councilRepository.getById(1);
        return CouncilResponse.from(council);
    }

    @Transactional
    public void updateCouncil(CouncilUpdateRequest request) {
        Council council = councilRepository.getById(1);
        CouncilOperatingHours councilOperatingHours = new CouncilOperatingHours(
            request.operatingHours().stream()
                .map(operatingHour -> operatingHour.toEntity(council))
                .toList()
        );
        List<CouncilSocialMedia> socialMedias = request.socialMedias().stream()
            .map(socialMedia -> socialMedia.toEntity(council))
            .toList();

        council.update(
            request.name(),
            request.introduce(),
            request.location(),
            request.personalColor(),
            request.phoneNumber(),
            request.email()
        );

        councilOperatingHourRepository.deleteByCouncilId(council.getId());
        councilSocialMediaRepository.deleteByCouncilId(council.getId());
        entityManager.flush();

        councilOperatingHours.operatingHours().forEach(councilOperatingHourRepository::save);
        socialMedias.forEach(councilSocialMediaRepository::save);
    }

    @Transactional
    public void deleteCouncil() {
        Council council = councilRepository.getById(1);

        councilOperatingHourRepository.deleteByCouncilId(council.getId());
        councilSocialMediaRepository.deleteByCouncilId(council.getId());
        councilRepository.deleteById(council.getId());
    }
}
