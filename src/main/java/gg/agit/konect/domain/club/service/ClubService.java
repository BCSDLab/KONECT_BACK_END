package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS;
import static gg.agit.konect.domain.club.enums.ClubPosition.PRESIDENT;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CLUB_MEMBER_ACCESS;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.group.model.GroupChatRoom;
import gg.agit.konect.domain.chat.group.repository.GroupChatRoomRepository;
import gg.agit.konect.domain.club.dto.ClubBasicInfoUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.MyManagedClubResponse;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubMembers;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubQueryRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubQueryRepository clubQueryRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubApplyRepository clubApplyRepository;
    private final UserRepository userRepository;
    private final ClubPermissionValidator clubPermissionValidator;
    private final GroupChatRoomRepository groupChatRoomRepository;

    public ClubsResponse getClubs(ClubCondition condition, Integer userId) {
        User user = userRepository.getById(userId);
        PageRequest pageable = PageRequest.of(condition.page() - 1, condition.limit());
        Page<ClubSummaryInfo> clubSummaryInfoPage = clubQueryRepository.findAllByFilter(
            pageable, condition.query(), condition.isRecruiting(), user.getUniversity().getId()
        );

        Set<Integer> pendingApprovalClubIds = findPendingApprovalClubIds(clubSummaryInfoPage, userId);
        return ClubsResponse.of(clubSummaryInfoPage, pendingApprovalClubIds);
    }

    private Set<Integer> findPendingApprovalClubIds(Page<ClubSummaryInfo> clubSummaryInfoPage, Integer userId) {
        List<Integer> clubIds = clubSummaryInfoPage.getContent().stream()
            .map(ClubSummaryInfo::id)
            .filter(Objects::nonNull)
            .toList();

        if (clubIds.isEmpty()) {
            return Set.of();
        }

        List<Integer> appliedClubIds = clubApplyRepository.findClubIdsByUserIdAndClubIdIn(userId, clubIds);
        if (appliedClubIds.isEmpty()) {
            return Set.of();
        }

        Set<Integer> pendingClubIds = new HashSet<>(appliedClubIds);
        List<Integer> memberClubIds = clubMemberRepository.findClubIdsByUserIdAndClubIdIn(userId, clubIds);
        pendingClubIds.removeAll(memberClubIds);

        return pendingClubIds;
    }

    public ClubDetailResponse getClubDetail(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        ClubMembers clubMembers = ClubMembers.from(clubMemberRepository.findAllByClubId(club.getId()));

        ClubMember president = clubMembers.getPresident();
        Integer memberCount = clubMembers.getCount();
        ClubRecruitment recruitment = club.getClubRecruitment();

        boolean isMember = clubMembers.contains(userId);
        Boolean isApplied = isMember || clubApplyRepository.existsByClubIdAndUserId(clubId, userId);

        return ClubDetailResponse.of(club, memberCount, recruitment, president, isMember, isApplied);
    }

    @Transactional
    public ClubDetailResponse createClub(Integer userId, ClubCreateRequest request) {
        User user = userRepository.getById(userId);
        Club club = request.toEntity(user.getUniversity());

        Club savedClub = clubRepository.save(club);

        groupChatRoomRepository.save(GroupChatRoom.of(savedClub));

        ClubMember president = ClubMember.builder()
            .club(savedClub)
            .user(user)
            .clubPosition(PRESIDENT)
            .isFeePaid(false)
            .build();

        clubMemberRepository.save(president);

        return getClubDetail(savedClub.getId(), userId);
    }

    @Transactional
    public void updateInfo(Integer clubId, Integer userId, ClubUpdateRequest request) {
        userRepository.getById(userId);
        Club club = clubRepository.getById(clubId);

        clubPermissionValidator.validateManagerAccess(clubId, userId);

        club.updateInfo(request.description(), request.imageUrl(), request.location(), request.introduce());
    }

    @Transactional
    public void updateBasicInfo(Integer clubId, Integer userId, ClubBasicInfoUpdateRequest request) {
        userRepository.getById(userId);
        Club club = clubRepository.getById(clubId);

        // TODO: 어드민 권한 체크 로직 추가 필요 (현재는 미구현)
        // if (!isAdmin(userId)) {
        //     throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        // }

        club.updateBasicInfo(request.name(), request.clubCategory());
    }

    public ClubMembershipsResponse getJoinedClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserId(userId);
        return ClubMembershipsResponse.from(clubMembers);
    }

    public ClubMembershipsResponse getManagedClubs(Integer userId) {
        User user = userRepository.getById(userId);
        if (user.isAdmin()) {
            List<Club> clubs = clubRepository.findAll();
            return ClubMembershipsResponse.forAdmin(clubs);
        }

        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserIdAndClubPositions(
            userId,
            MANAGERS
        );
        return ClubMembershipsResponse.from(clubMembers);
    }

    public MyManagedClubResponse getManagedClubDetail(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);
        if (user.isAdmin()) {
            return MyManagedClubResponse.forAdmin(club, user);
        }

        clubPermissionValidator.validateManagerAccess(clubId, userId);

        ClubMember clubMember = clubMemberRepository.getByClubIdAndUserId(clubId, userId);
        return MyManagedClubResponse.from(club, clubMember);
    }

    public ClubMembersResponse getClubMembers(Integer clubId, Integer userId, ClubMemberCondition condition) {
        User user = userRepository.getById(userId);
        if (!user.isAdmin()) {
            boolean isMember = clubMemberRepository.existsByClubIdAndUserId(clubId, userId);
            if (!isMember) {
                throw CustomException.of(FORBIDDEN_CLUB_MEMBER_ACCESS);
            }
        }

        List<ClubMember> clubMembers;
        if (condition != null && condition.position() != null) {
            clubMembers = clubMemberRepository.findAllByClubIdAndPosition(clubId, condition.position());
        } else {
            clubMembers = clubMemberRepository.findAllByClubId(clubId);
        }

        return ClubMembersResponse.from(clubMembers);
    }

}
