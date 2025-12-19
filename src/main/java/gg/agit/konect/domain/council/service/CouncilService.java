package gg.agit.konect.domain.council.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.council.dto.CouncilCreateRequest;
import gg.agit.konect.domain.council.dto.CouncilResponse;
import gg.agit.konect.domain.council.dto.CouncilUpdateRequest;
import gg.agit.konect.domain.council.model.Council;
import gg.agit.konect.domain.council.repository.CouncilRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouncilService {

    private final CouncilRepository councilRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createCouncil(Integer userId, CouncilCreateRequest request) {
        User user = userRepository.getById(userId);
        University university = user.getUniversity();

        if (councilRepository.findByUniversity(university).isPresent()) {
            throw CustomException.of(ApiResponseCode.ALREADY_EXIST_COUNCIL);
        }

        Council council = request.toEntity(university);

        councilRepository.save(council);
    }

    public CouncilResponse getCouncil(Integer userId) {
        User user = userRepository.getById(userId);
        University university = user.getUniversity();
        Council council = councilRepository.getByUniversity(university);

        return CouncilResponse.from(council);
    }

    @Transactional
    public void updateCouncil(Integer userId, CouncilUpdateRequest request) {
        User user = userRepository.getById(userId);
        University university = user.getUniversity();
        Council council = councilRepository.getByUniversity(university);

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
    public void deleteCouncil(Integer userId) {
        User user = userRepository.getById(userId);
        University university = user.getUniversity();
        Council council = councilRepository.getByUniversity(university);

        councilRepository.deleteById(council.getId());
    }
}
