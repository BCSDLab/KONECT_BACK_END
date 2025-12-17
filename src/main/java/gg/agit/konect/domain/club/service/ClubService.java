package gg.agit.konect.domain.club.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.JoinedClubsResponse;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubQueryRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubQueryRepository clubQueryRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRecruitmentRepository clubRecruitmentRepository;

    public ClubsResponse getClubs(Integer page, Integer limit, String query, Boolean isRecruiting) {
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ClubSummaryInfo> clubSummaryInfoPage = clubQueryRepository.findAllByFilter(pageable, query, isRecruiting);
        return ClubsResponse.of(clubSummaryInfoPage);
    }

    public ClubDetailResponse getClubDetail(Integer clubId) {
        Club club = clubRepository.getById(clubId);
        List<ClubMember> clubMembers = clubMemberRepository.findAllByClubId(club.getId());
        List<ClubMember> clubPresidents = clubMembers.stream()
            .filter(ClubMember::isPresident)
            .toList();
        Integer memberCount = clubMembers.size();
        ClubRecruitment recruitment = clubRecruitmentRepository.findByClubId(clubId).orElse(null);

        return ClubDetailResponse.of(club, memberCount, recruitment, clubPresidents);
    }

    public JoinedClubsResponse getJoinedClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserId(userId);
        return JoinedClubsResponse.of(clubMembers);
    }

    public ClubMembersResponse getClubMembers(Integer clubId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByClubId(clubId);
        return ClubMembersResponse.from(clubMembers);
    }
}
