package com.example.konect.club.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.konect.club.dto.ClubDetailResponse;
import com.example.konect.club.dto.ClubsResponse;
import com.example.konect.club.model.Club;
import com.example.konect.club.model.ClubMember;
import com.example.konect.club.model.ClubRecruitment;
import com.example.konect.club.model.ClubRepresentative;
import com.example.konect.club.model.ClubSummaryInfo;
import com.example.konect.club.repository.ClubMemberRepository;
import com.example.konect.club.repository.ClubQueryRepository;
import com.example.konect.club.repository.ClubRecruitmentRepository;
import com.example.konect.club.repository.ClubRepository;
import com.example.konect.club.repository.ClubRepresentativeRepository;

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

}
