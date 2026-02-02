package gg.agit.konect.domain.club.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpsertRequest;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubRecruitmentImage;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClubRecruitmentService {

    private final ClubRepository clubRepository;
    private final ClubRecruitmentRepository clubRecruitmentRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubApplyRepository clubApplyRepository;
    private final UserRepository userRepository;
    private final ClubPermissionValidator clubPermissionValidator;

    public ClubRecruitmentResponse getRecruitment(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);
        ClubRecruitment recruitment = clubRecruitmentRepository.getByClubId(club.getId());
        boolean isMember = clubMemberRepository.existsByClubIdAndUserId(clubId, userId);
        boolean isApplied = isMember || clubApplyRepository.existsByClubIdAndUserId(club.getId(), user.getId());

        return ClubRecruitmentResponse.of(recruitment, isApplied);
    }

    @Transactional
    public void upsertRecruitment(Integer clubId, Integer userId, ClubRecruitmentUpsertRequest request) {
        Club club = clubRepository.getById(clubId);
        userRepository.getById(userId);

        clubPermissionValidator.validateManagerAccess(clubId, userId);

        ClubRecruitment clubRecruitment = clubRecruitmentRepository.findByClubId(clubId)
            .orElseGet(() -> ClubRecruitment.of(
                request.startDate(),
                request.endDate(),
                request.isAlwaysRecruiting(),
                request.content(),
                club
            ));

        if (clubRecruitment.getId() != null) {
            clubRecruitment.update(
                request.startDate(),
                request.endDate(),
                request.isAlwaysRecruiting(),
                request.content()
            );

            clubRecruitment.getImages().clear();
        }

        List<String> imageUrls = request.getImageUrls();
        for (int index = 0; index < imageUrls.size(); index++) {
            ClubRecruitmentImage image = ClubRecruitmentImage.of(
                imageUrls.get(index),
                index,
                clubRecruitment
            );
            clubRecruitment.addImage(image);
        }

        if (clubRecruitment.getId() == null) {
            clubRecruitmentRepository.save(clubRecruitment);
        }
    }
}
