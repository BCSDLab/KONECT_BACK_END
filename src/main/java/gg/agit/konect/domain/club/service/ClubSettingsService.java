package gg.agit.konect.domain.club.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.bank.repository.BankRepository;
import gg.agit.konect.domain.club.dto.ClubSettingsResponse;
import gg.agit.konect.domain.club.dto.ClubSettingsResponse.ApplicationSummary;
import gg.agit.konect.domain.club.dto.ClubSettingsResponse.FeeSummary;
import gg.agit.konect.domain.club.dto.ClubSettingsResponse.RecruitmentSummary;
import gg.agit.konect.domain.club.dto.ClubSettingsUpdateRequest;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubApplyQuestionRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubSettingsService {

    private final ClubRepository clubRepository;
    private final ClubRecruitmentRepository clubRecruitmentRepository;
    private final ClubApplyQuestionRepository clubApplyQuestionRepository;
    private final BankRepository bankRepository;
    private final UserRepository userRepository;
    private final ClubPermissionValidator clubPermissionValidator;

    public ClubSettingsResponse getSettings(Integer clubId, Integer userId) {
        userRepository.getById(userId);
        Club club = clubRepository.getById(clubId);

        clubPermissionValidator.validateManagerAccess(clubId, userId);

        RecruitmentSummary recruitmentSummary = buildRecruitmentSummary(clubId);
        ApplicationSummary applicationSummary = buildApplicationSummary(clubId);
        FeeSummary feeSummary = buildFeeSummary(club);

        return new ClubSettingsResponse(
            Boolean.TRUE.equals(club.getIsRecruitmentEnabled()),
            Boolean.TRUE.equals(club.getIsApplicationEnabled()),
            Boolean.TRUE.equals(club.getIsFeeRequired()),
            recruitmentSummary,
            applicationSummary,
            feeSummary
        );
    }

    @Transactional
    public ClubSettingsResponse updateSettings(
        Integer clubId,
        Integer userId,
        ClubSettingsUpdateRequest request
    ) {
        userRepository.getById(userId);
        Club club = clubRepository.getById(clubId);

        clubPermissionValidator.validateManagerAccess(clubId, userId);

        club.updateSettings(
            request.isRecruitmentEnabled(),
            request.isApplicationEnabled(),
            request.isFeeEnabled()
        );

        RecruitmentSummary recruitmentSummary = buildRecruitmentSummary(clubId);
        ApplicationSummary applicationSummary = buildApplicationSummary(clubId);
        FeeSummary feeSummary = buildFeeSummary(club);

        return new ClubSettingsResponse(
            Boolean.TRUE.equals(club.getIsRecruitmentEnabled()),
            Boolean.TRUE.equals(club.getIsApplicationEnabled()),
            Boolean.TRUE.equals(club.getIsFeeRequired()),
            recruitmentSummary,
            applicationSummary,
            feeSummary
        );
    }

    private RecruitmentSummary buildRecruitmentSummary(Integer clubId) {
        return clubRecruitmentRepository.findByClubId(clubId)
            .map(recruitment -> new RecruitmentSummary(
                recruitment.getStartDate(),
                recruitment.getEndDate(),
                Boolean.TRUE.equals(recruitment.getIsAlwaysRecruiting())
            ))
            .orElse(null);
    }

    private ApplicationSummary buildApplicationSummary(Integer clubId) {
        int questionCount = clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(clubId).size();
        return new ApplicationSummary(questionCount);
    }

    private FeeSummary buildFeeSummary(Club club) {
        if (!StringUtils.hasText(club.getFeeBank())) {
            return null;
        }

        Integer bankId = resolveBankId(club.getFeeBank());

        return new FeeSummary(
            club.getFeeAmount(),
            bankId,
            club.getFeeBank(),
            club.getFeeAccountNumber(),
            club.getFeeAccountHolder()
        );
    }

    private Integer resolveBankId(String bankName) {
        if (!StringUtils.hasText(bankName)) {
            return null;
        }
        return bankRepository.getByName(bankName).getId();
    }
}
