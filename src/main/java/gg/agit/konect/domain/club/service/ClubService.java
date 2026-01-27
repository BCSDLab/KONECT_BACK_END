package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPositionGroup.*;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.bank.repository.BankRepository;
import gg.agit.konect.domain.club.dto.ClubApplicationAnswersResponse;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubAppliedClubsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubBasicInfoUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubDetailUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMemberCondition;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubProfileUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentCreateRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubTagsResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubApplyQuestionAnswers;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubMembers;
import gg.agit.konect.domain.club.model.ClubPosition;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubRecruitmentImage;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import gg.agit.konect.domain.club.model.ClubTag;
import gg.agit.konect.domain.club.model.ClubTagMap;
import gg.agit.konect.domain.club.repository.ClubApplyAnswerRepository;
import gg.agit.konect.domain.club.repository.ClubApplyQuestionRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPositionRepository;
import gg.agit.konect.domain.club.repository.ClubQueryRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.repository.ClubTagMapRepository;
import gg.agit.konect.domain.club.repository.ClubTagRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private static final Set<ClubPositionGroup> PRESIDENT_ALLOWED_GROUPS =
        EnumSet.of(PRESIDENT);
    private static final Set<ClubPositionGroup> MANAGER_ALLOWED_GROUPS =
        EnumSet.of(PRESIDENT, MANAGER);
    private static final Set<ClubPositionGroup> LEADER_ALLOWED_GROUPS =
        EnumSet.of(PRESIDENT, VICE_PRESIDENT);

    private final ClubQueryRepository clubQueryRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPositionRepository clubPositionRepository;
    private final ClubRecruitmentRepository clubRecruitmentRepository;
    private final ClubApplyRepository clubApplyRepository;
    private final ClubApplyQuestionRepository clubApplyQuestionRepository;
    private final ClubApplyAnswerRepository clubApplyAnswerRepository;
    private final UserRepository userRepository;
    private final BankRepository bankRepository;
    private final ClubTagRepository clubTagRepository;
    private final ClubTagMapRepository clubTagMapRepository;

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
        List<ClubTagMap> clubTagMaps = clubTagMapRepository.findAllByClubId(clubId);

        boolean isMember = clubMembers.contains(userId);
        Boolean isApplied = isMember || clubApplyRepository.existsByClubIdAndUserId(clubId, userId);

        return ClubDetailResponse.of(club, memberCount, recruitment, president, clubTagMaps, isMember, isApplied);
    }

    @Transactional
    public ClubDetailResponse createClub(Integer userId, ClubCreateRequest request) {
        User user = userRepository.getById(userId);
        Club club = request.toEntity(user.getUniversity());

        Club savedClub = clubRepository.save(club);

        List<ClubPosition> defaultPositions = Arrays.stream(ClubPositionGroup.values())
            .map(group -> ClubPosition.builder()
                .name(group.getDescription())
                .clubPositionGroup(group)
                .club(savedClub)
                .build())
            .toList();

        defaultPositions.forEach(clubPositionRepository::save);

        ClubPosition presidentPosition = defaultPositions.get(0);
        ClubMember president = ClubMember.builder()
            .club(savedClub)
            .user(user)
            .clubPosition(presidentPosition)
            .isFeePaid(false)
            .build();

        clubMemberRepository.save(president);

        return getClubDetail(savedClub.getId(), userId);
    }

    @Transactional
    public void updateProfile(Integer clubId, Integer userId, ClubProfileUpdateRequest request) {
        userRepository.getById(userId);
        Club club = clubRepository.getById(clubId);

        if (!hasClubManageAccess(clubId, userId, MANAGER_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        club.updateProfile(request.introduce(), request.imageUrl());

        clubTagMapRepository.deleteByClubId(clubId);

        List<ClubTag> tags = clubTagRepository.findAllByIdIn(request.tagIds());
        if (tags.size() != request.tagIds().size()) {
            throw CustomException.of(NOT_FOUND_CLUB_TAG);
        }

        tags.forEach(tag -> {
            ClubTagMap tagMap = ClubTagMap.builder()
                .club(club)
                .tag(tag)
                .build();
            clubTagMapRepository.save(tagMap);
        });
    }

    @Transactional
    public void updateDetails(Integer clubId, Integer userId, ClubDetailUpdateRequest request) {
        userRepository.getById(userId);
        Club club = clubRepository.getById(clubId);

        if (!hasClubManageAccess(clubId, userId, MANAGER_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        club.updateDetails(request.location(), request.introduce());
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

    public ClubTagsResponse getTags() {
        List<ClubTag> tags = clubTagRepository.findAll();
        return ClubTagsResponse.from(tags);
    }

    public ClubMembershipsResponse getJoinedClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserId(userId);
        return ClubMembershipsResponse.from(clubMembers);
    }

    public ClubMembershipsResponse getManagedClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserIdAndClubPosition(userId, PRESIDENT);
        return ClubMembershipsResponse.from(clubMembers);
    }

    public ClubAppliedClubsResponse getAppliedClubs(Integer userId) {
        List<ClubApply> clubApplies = clubApplyRepository.findAllPendingByUserIdWithClub(userId);
        return ClubAppliedClubsResponse.from(clubApplies);
    }

    public ClubApplicationsResponse getClubApplications(Integer clubId, Integer userId) {
        clubRepository.getById(clubId);

        if (!hasClubManageAccess(clubId, userId, MANAGER_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        ClubRecruitment recruitment = clubRecruitmentRepository.getByClubId(clubId);
        List<ClubApply> clubApplies = findApplicationsByRecruitmentPeriod(clubId, recruitment);

        return ClubApplicationsResponse.from(clubApplies);
    }

    public ClubApplicationAnswersResponse getClubApplicationAnswers(
        Integer clubId,
        Integer applicationId,
        Integer userId
    ) {
        clubRepository.getById(clubId);

        if (!hasClubManageAccess(clubId, userId, MANAGER_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        ClubApply clubApply = clubApplyRepository.getByIdAndClubId(applicationId, clubId);
        List<ClubApplyQuestion> questions =
            clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(clubId);
        List<ClubApplyAnswer> answers = clubApplyAnswerRepository.findAllByApplyIdWithQuestion(applicationId);

        return ClubApplicationAnswersResponse.of(clubApply, questions, answers);
    }

    @Transactional
    public void approveClubApplication(Integer clubId, Integer applicationId, Integer userId) {
        Club club = clubRepository.getById(clubId);

        if (!hasClubManageAccess(clubId, userId, LEADER_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        ClubApply clubApply = clubApplyRepository.getByIdAndClubId(applicationId, clubId);
        User applicant = clubApply.getUser();

        if (clubMemberRepository.existsByClubIdAndUserId(clubId, applicant.getId())) {
            throw CustomException.of(ALREADY_CLUB_MEMBER);
        }

        ClubPosition memberPosition = clubPositionRepository.getFirstByClubIdAndClubPositionGroup(clubId, MEMBER);

        ClubMember newMember = ClubMember.builder()
            .club(club)
            .user(applicant)
            .clubPosition(memberPosition)
            .isFeePaid(true)
            .build();

        clubMemberRepository.save(newMember);
        clubApplyRepository.delete(clubApply);
    }

    private List<ClubApply> findApplicationsByRecruitmentPeriod(
        Integer clubId,
        ClubRecruitment recruitment
    ) {
        if (recruitment.getIsAlwaysRecruiting()) {
            return clubApplyRepository.findAllByClubIdWithUser(clubId);
        }

        LocalDateTime startDateTime = recruitment.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = recruitment.getEndDate().atTime(LocalTime.MAX);

        return clubApplyRepository.findAllByClubIdAndCreatedAtBetweenWithUser(
            clubId,
            startDateTime,
            endDateTime
        );
    }

    public ClubMembersResponse getClubMembers(Integer clubId, Integer userId, ClubMemberCondition condition) {
        boolean isMember = clubMemberRepository.existsByClubIdAndUserId(clubId, userId);
        if (!isMember) {
            throw CustomException.of(FORBIDDEN_CLUB_MEMBER_ACCESS);
        }

        List<ClubMember> clubMembers;
        if (condition != null && condition.positionGroup() != null) {
            clubMembers = clubMemberRepository.findAllByClubIdAndPositionGroup(clubId, condition.positionGroup());
        } else {
            clubMembers = clubMemberRepository.findAllByClubId(clubId);
        }

        return ClubMembersResponse.from(clubMembers);
    }

    public ClubFeeInfoResponse getFeeInfo(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        userRepository.getById(userId);

        boolean isApplied = clubApplyRepository.existsByClubIdAndUserId(clubId, userId);
        boolean isManager = clubMemberRepository.existsByClubIdAndUserIdAndPositionGroupIn(
            clubId,
            userId,
            MANAGER_ALLOWED_GROUPS
        );

        if (!isApplied && !isManager) {
            throw CustomException.of(FORBIDDEN_CLUB_FEE_INFO);
        }

        return ClubFeeInfoResponse.from(club);
    }

    @Transactional
    public ClubFeeInfoResponse replaceFeeInfo(Integer clubId, Integer userId, ClubFeeInfoReplaceRequest request) {
        userRepository.getById(userId);
        Club club = clubRepository.getById(clubId);

        if (!hasClubManageAccess(clubId, userId, MANAGER_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        String bankName = bankRepository.getById(request.bankId()).getName();

        club.replaceFeeInfo(
            request.amount(),
            bankName,
            request.accountNumber(),
            request.accountHolder(),
            request.deadLine()
        );

        return ClubFeeInfoResponse.from(club);
    }

    public ClubApplyQuestionsResponse getApplyQuestions(Integer clubId, Integer userId) {
        User user = userRepository.getById(userId);
        List<ClubApplyQuestion> questions =
            clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(clubId);

        return ClubApplyQuestionsResponse.from(questions);
    }

    @Transactional
    public ClubApplyQuestionsResponse replaceApplyQuestions(
        Integer clubId,
        Integer userId,
        ClubApplyQuestionsReplaceRequest request
    ) {
        Club club = clubRepository.getById(clubId);

        if (!hasClubManageAccess(clubId, userId, MANAGER_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        List<ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest> questionRequests = request.questions();
        Set<Integer> requestedQuestionIds = new HashSet<>();

        List<ClubApplyQuestion> existingQuestions =
            clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(clubId);
        Map<Integer, ClubApplyQuestion> existingQuestionMap = existingQuestions.stream()
            .collect(Collectors.toMap(ClubApplyQuestion::getId, question -> question));

        updateQuestions(existingQuestionMap, questionRequests, requestedQuestionIds);

        List<ClubApplyQuestion> questionsToCreate = createQuestions(club, questionRequests);

        deleteQuestions(existingQuestions, requestedQuestionIds);

        if (!questionsToCreate.isEmpty()) {
            clubApplyQuestionRepository.saveAll(questionsToCreate);
        }

        List<ClubApplyQuestion> questions =
            clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(clubId);

        return ClubApplyQuestionsResponse.from(questions);
    }

    public ClubRecruitmentResponse getRecruitment(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);
        ClubRecruitment recruitment = clubRecruitmentRepository.getByClubId(club.getId());
        boolean isMember = clubMemberRepository.existsByClubIdAndUserId(clubId, userId);
        boolean isApplied = isMember || clubApplyRepository.existsByClubIdAndUserId(club.getId(), user.getId());

        return ClubRecruitmentResponse.of(recruitment, isApplied);
    }

    @Transactional
    public ClubFeeInfoResponse applyClub(Integer clubId, Integer userId, ClubApplyRequest request) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);

        if (clubApplyRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ALREADY_APPLIED_CLUB);
        }

        List<ClubApplyQuestion> questions =
            clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(clubId);
        ClubApplyQuestionAnswers answers = ClubApplyQuestionAnswers.of(questions, request.toAnswerMap());

        ClubApply apply = clubApplyRepository.save(ClubApply.of(club, user));

        List<ClubApplyAnswer> applyAnswers = answers.toEntities(apply);

        if (!applyAnswers.isEmpty()) {
            clubApplyAnswerRepository.saveAll(applyAnswers);
        }

        return ClubFeeInfoResponse.from(club);
    }

    private List<ClubApplyQuestion> createQuestions(
        Club club,
        List<ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest> questionRequests
    ) {
        List<ClubApplyQuestion> questionsToCreate = new ArrayList<>();

        for (ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest questionRequest : questionRequests) {
            if (questionRequest.questionId() != null) {
                continue;
            }

            questionsToCreate.add(ClubApplyQuestion.of(
                club,
                questionRequest.question(),
                questionRequest.isRequired())
            );
        }

        return questionsToCreate;
    }

    private void updateQuestions(
        Map<Integer, ClubApplyQuestion> existingQuestionMap,
        List<ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest> questionRequests,
        Set<Integer> requestedQuestionIds
    ) {
        for (ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest questionRequest : questionRequests) {
            Integer questionId = questionRequest.questionId();

            if (questionId == null) {
                continue;
            }

            if (!requestedQuestionIds.add(questionId)) {
                throw CustomException.of(DUPLICATE_CLUB_APPLY_QUESTION);
            }

            ClubApplyQuestion existingQuestion = existingQuestionMap.get(questionId);

            if (existingQuestion == null) {
                throw CustomException.of(NOT_FOUND_CLUB_APPLY_QUESTION);
            }

            existingQuestion.update(
                questionRequest.question(),
                questionRequest.isRequired()
            );
        }
    }

    private void deleteQuestions(
        List<ClubApplyQuestion> existingQuestions,
        Set<Integer> requestedQuestionIds
    ) {
        List<ClubApplyQuestion> questionsToDelete = existingQuestions.stream()
            .filter(question -> !requestedQuestionIds.contains(question.getId()))
            .toList();

        if (!questionsToDelete.isEmpty()) {
            clubApplyQuestionRepository.deleteAll(questionsToDelete);
        }
    }

    @Transactional
    public void createRecruitment(Integer clubId, Integer userId, ClubRecruitmentCreateRequest request) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);

        if (!hasClubManageAccess(clubId, userId, PRESIDENT_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_RECRUITMENT_CREATE);
        }

        if (clubRecruitmentRepository.existsByClubId(clubId)) {
            throw CustomException.of(ALREADY_EXIST_CLUB_RECRUITMENT);
        }

        ClubRecruitment clubRecruitment = ClubRecruitment.of(
            request.startDate(),
            request.endDate(),
            request.isAlwaysRecruiting(),
            request.content(),
            club
        );
        List<String> imageUrls = request.getImageUrls();
        for (int index = 0; index < imageUrls.size(); index++) {
            ClubRecruitmentImage clubRecruitmentImage = ClubRecruitmentImage.of(
                imageUrls.get(index),
                index,
                clubRecruitment
            );
            clubRecruitment.addImage(clubRecruitmentImage);
        }

        clubRecruitmentRepository.save(clubRecruitment);
    }

    @Transactional
    public void updateRecruitment(Integer clubId, Integer userId, ClubRecruitmentUpdateRequest request) {
        clubRepository.getById(clubId);
        userRepository.getById(userId);

        if (!hasClubManageAccess(clubId, userId, MANAGER_ALLOWED_GROUPS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        ClubRecruitment clubRecruitment = clubRecruitmentRepository.getByClubId(clubId);
        clubRecruitment.update(
            request.startDate(),
            request.endDate(),
            request.isAlwaysRecruiting(),
            request.content()
        );

        clubRecruitment.getImages().clear();
        List<String> imageUrls = request.getImageUrls();
        for (int index = 0; index < imageUrls.size(); index++) {
            ClubRecruitmentImage newImage = ClubRecruitmentImage.of(
                imageUrls.get(index),
                index,
                clubRecruitment
            );
            clubRecruitment.addImage(newImage);
        }
    }

    private boolean hasClubManageAccess(
        Integer clubId,
        Integer userId,
        Set<ClubPositionGroup> allowedGroups
    ) {
        return clubMemberRepository.existsByClubIdAndUserIdAndPositionGroupIn(
            clubId,
            userId,
            allowedGroups
        );
    }
}
