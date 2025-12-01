package com.example.konect.club.service;

import static com.example.konect.club.enums.PositionGroup.MEMBER;
import static com.example.konect.club.enums.PositionGroup.PRESIDENT;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.konect.club.dto.ClubDetailResponse;
import com.example.konect.club.dto.ClubsResponse;
import com.example.konect.club.dto.JoinedClubsResponse;
import com.example.konect.club.model.Club;
import com.example.konect.club.model.ClubExecutive;
import com.example.konect.club.model.ClubRecruitment;
import com.example.konect.club.model.ClubSummaryInfo;
import com.example.konect.club.repository.ClubExecutiveRepository;
import com.example.konect.club.repository.ClubMemberRepository;
import com.example.konect.club.repository.ClubQueryRepository;
import com.example.konect.club.repository.ClubRecruitmentRepository;
import com.example.konect.club.repository.ClubRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubQueryRepository clubQueryRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRecruitmentRepository clubRecruitmentRepository;
    private final ClubExecutiveRepository clubExecutiveRepository;

    public ClubsResponse getClubs(Integer page, Integer limit, String query, Boolean isRecruiting) {
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ClubSummaryInfo> clubSummaryInfoPage = clubQueryRepository.findAllByFilter(pageable, query, isRecruiting);
        return ClubsResponse.of(clubSummaryInfoPage);
    }

    public ClubDetailResponse getClubDetail(Integer clubId) {
        Club club = clubRepository.getById(clubId);
        Long memberCount = clubMemberRepository.countByClubId(clubId);
        ClubRecruitment recruitment = clubRecruitmentRepository.findByClubId(clubId).orElse(null);
        List<ClubExecutive> representatives = clubExecutiveRepository.findByClubIdAndIsRepresentative(clubId, true);

        return ClubDetailResponse.of(club, memberCount, recruitment, representatives);
    }

    public JoinedClubsResponse getJoinedClubs() {
        return new JoinedClubsResponse(List.of(
            new JoinedClubsResponse.InnerJoinedClubResponse(1, "BCSD", "https://static.koreatech.in/upload/CLUB/2025/6/10/d0320625-7055-4a33-aad7-ee852a008ce7/BCSD Logo-symbol.png", "학술", "회장", PRESIDENT, true),
            new JoinedClubsResponse.InnerJoinedClubResponse(2, "CUT", "https://static.koreatech.in/upload/LOST_ITEMS/2025/6/12/bbacbbb4-5f64-4582-8f5f-e6e446031362/1000035027.jpg", "운동", "일반회원", MEMBER, false)
        ));
    }
}
