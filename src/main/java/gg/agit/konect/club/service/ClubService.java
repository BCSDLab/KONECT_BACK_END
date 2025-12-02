package gg.agit.konect.club.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.club.dto.ClubDetailResponse;
import gg.agit.konect.club.dto.ClubsResponse;
import gg.agit.konect.club.dto.JoinedClubsResponse;
import gg.agit.konect.club.model.Club;
import gg.agit.konect.club.model.ClubFeePayment;
import gg.agit.konect.club.model.ClubMember;
import gg.agit.konect.club.model.ClubRecruitment;
import gg.agit.konect.club.model.ClubRepresentative;
import gg.agit.konect.club.model.ClubSummaryInfo;
import gg.agit.konect.club.repository.ClubFeePaymentRepository;
import gg.agit.konect.club.repository.ClubMemberRepository;
import gg.agit.konect.club.repository.ClubQueryRepository;
import gg.agit.konect.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.club.repository.ClubRepository;
import gg.agit.konect.club.repository.ClubRepresentativeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubQueryRepository clubQueryRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRecruitmentRepository clubRecruitmentRepository;
    private final ClubRepresentativeRepository clubRepresentativeRepository;
    private final ClubFeePaymentRepository clubFeePaymentRepository;

    public ClubsResponse getClubs(Integer page, Integer limit, String query, Boolean isRecruiting) {
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ClubSummaryInfo> clubSummaryInfoPage = clubQueryRepository.findAllByFilter(pageable, query, isRecruiting);
        return ClubsResponse.of(clubSummaryInfoPage);
    }

    public ClubDetailResponse getClubDetail(Integer clubId) {
        Club club = clubRepository.getById(clubId);
        Long memberCount = clubMemberRepository.countByClubId(clubId);
        ClubRecruitment recruitment = clubRecruitmentRepository.findByClubId(clubId).orElse(null);
        List<ClubRepresentative> representatives = clubRepresentativeRepository.findByClubId(clubId);

        List<ClubRepresentative> validRepresentatives = validateRepresentatives(clubId, representatives);

        return ClubDetailResponse.of(club, memberCount, recruitment, validRepresentatives);
    }

    private List<ClubRepresentative> validateRepresentatives(
        Integer clubId,
        List<ClubRepresentative> representatives
    ) {
        return representatives.stream()
            .filter(rep -> {
                ClubMember member = clubMemberRepository.getByClubIdAndUserId(
                    clubId,
                    rep.getUser().getId()
                );
                return Boolean.TRUE.equals(member.getIsAdmin());
            })
            .toList();
    }

    public JoinedClubsResponse getJoinedClubs() {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserId(1);
        List<ClubFeePayment> clubFeePayments = clubFeePaymentRepository.findAllByClubMemberIn(clubMembers);
        Map<Integer, Boolean> clubFeePaymentMap = getUnpaidStatusMapByClubId(clubFeePayments);
        fillMissingClubFeeStatuses(clubMembers, clubFeePaymentMap);
        return JoinedClubsResponse.of(clubMembers, clubFeePaymentMap);
    }

    private Map<Integer, Boolean> getUnpaidStatusMapByClubId(List<ClubFeePayment> clubFeePayments) {
        Map<Integer, Boolean> clubFeePaymentMap = new HashMap<>();
        for (ClubFeePayment clubFeePayment : clubFeePayments) {
            Integer clubId = clubFeePayment.getClubMember().getId().getClubId();
            clubFeePaymentMap.put(clubId, clubFeePayment.isUnPaid());
        }
        return clubFeePaymentMap;
    }

    private void fillMissingClubFeeStatuses(List<ClubMember> clubMembers, Map<Integer, Boolean> clubFeePaymentMap) {
        for (ClubMember clubMember : clubMembers) {
            Integer clubId = clubMember.getClub().getId();
            clubFeePaymentMap.putIfAbsent(clubId, false);
        }
    }
}
