package gg.agit.konect.council.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.council.dto.CouncilCreateRequest;
import gg.agit.konect.council.dto.CouncilResponse;
import gg.agit.konect.council.dto.CouncilUpdateRequest;
import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.repository.CouncilOperatingHourRepository;
import gg.agit.konect.council.repository.CouncilRepository;
import gg.agit.konect.council.repository.CouncilSocialMediaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouncilService {

    private final CouncilRepository councilRepository;
    private final CouncilOperatingHourRepository councilOperatingHourRepository;
    private final CouncilSocialMediaRepository councilSocialMediaRepository;

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
        council.update(
            request.name(),
            request.introduce(),
            request.location(),
            request.personalColor(),
            request.instagramUrl(),
            request.operatingHour()
        );
    }

    @Transactional
    public void deleteCouncil() {
        Council council = councilRepository.getById(1);

        councilOperatingHourRepository.deleteByCouncilId(council.getId());
        councilSocialMediaRepository.deleteByCouncilId(council.getId());
        councilRepository.deleteById(council.getId());
    }
}
