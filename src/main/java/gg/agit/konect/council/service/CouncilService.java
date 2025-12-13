package gg.agit.konect.council.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.council.dto.CouncilCreateRequest;
import gg.agit.konect.council.dto.CouncilResponse;
import gg.agit.konect.council.dto.CouncilUpdateRequest;
import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.repository.CouncilRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouncilService {

    private final CouncilRepository councilRepository;

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
            request.imageUrl(),
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
        councilRepository.deleteById(council.getId());
    }
}
