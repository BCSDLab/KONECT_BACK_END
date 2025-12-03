package gg.agit.konect.council.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.council.dto.CouncilResponse;
import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.model.CouncilOperatingHour;
import gg.agit.konect.council.model.CouncilSocialMedia;
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

    public CouncilResponse getCouncil() {
        Council council = councilRepository.getById(1);
        List<CouncilOperatingHour> operatingHours = councilOperatingHourRepository.findByCouncilId(council.getId());
        List<CouncilSocialMedia> socialMedias = councilSocialMediaRepository.findByCouncilId(council.getId());

        return CouncilResponse.of(council, operatingHours, socialMedias);
    }
}
