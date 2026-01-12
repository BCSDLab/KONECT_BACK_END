package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPositionGroup.PRESIDENT;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubMembershipsResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentCreateRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubApplyQuestionAnswers;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubMembers;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubRecruitmentImage;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import gg.agit.konect.domain.club.repository.ClubApplyAnswerRepository;
import gg.agit.konect.domain.club.repository.ClubApplyQuestionRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubQueryRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
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
    private final ClubRecruitmentRepository clubRecruitmentRepository;
    private final ClubApplyRepository clubApplyRepository;
    private final ClubApplyQuestionRepository clubApplyQuestionRepository;
    private final ClubApplyAnswerRepository clubApplyAnswerRepository;
    private final UserRepository userRepository;

    public ClubsResponse getClubs(ClubCondition condition, Integer userId) {
        User user = userRepository.getById(userId);
        PageRequest pageable = PageRequest.of(condition.page() - 1, condition.limit());
        Page<ClubSummaryInfo> clubSummaryInfoPage = clubQueryRepository.findAllByFilter(
            pageable, condition.query(), condition.isRecruiting(), user.getUniversity().getId()
        );
        return ClubsResponse.of(clubSummaryInfoPage);
    }

    public ClubDetailResponse getClubDetail(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        ClubMembers clubMembers = ClubMembers.from(clubMemberRepository.findAllByClubId(club.getId()));

        List<ClubMember> clubPresidents = clubMembers.getPresidents();
        Integer memberCount = clubMembers.getCount();
        ClubRecruitment recruitment = club.getClubRecruitment();

        boolean isMember = clubMembers.contains(userId);
        Boolean isApplied = isMember || clubApplyRepository.existsByClubIdAndUserId(clubId, userId);

        return ClubDetailResponse.of(club, memberCount, recruitment, clubPresidents, isMember, isApplied);
    }

    public ClubMembershipsResponse getJoinedClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserId(userId);
        return ClubMembershipsResponse.from(clubMembers);
    }

    public ClubMembershipsResponse getManagedClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserIdAndClubPosition(userId, PRESIDENT);
        return ClubMembershipsResponse.from(clubMembers);
    }

    public ClubMembersResponse getClubMembers(Integer clubId, Integer userId) {
        boolean isMember = clubMemberRepository.existsByClubIdAndUserId(clubId, userId);
        if (!isMember) {
            throw CustomException.of(FORBIDDEN_CLUB_MEMBER_ACCESS);
        }

        List<ClubMember> clubMembers = clubMemberRepository.findAllByClubId(clubId);
        return ClubMembersResponse.from(clubMembers);
    }

    public ClubFeeInfoResponse getFeeInfo(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);

        if (!clubApplyRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(FORBIDDEN_CLUB_FEE_INFO);
        }

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

        ClubMember clubMember = clubMemberRepository.findByClubIdAndUserId(club.getId(), user.getId())
            .orElseThrow(() -> CustomException.of(FORBIDDEN_CLUB_RECRUITMENT_CREATE));

        validateClubManager(clubMember);

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

    private void validateClubManager(ClubMember clubMember) {
        if (!clubMember.isPresident()) {
            throw CustomException.of(FORBIDDEN_CLUB_RECRUITMENT_CREATE);
        }
    }

    @Transactional
    public void updateRecruitment(Integer clubId, Integer userId, ClubRecruitmentUpdateRequest request) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);

        ClubMember clubMember = clubMemberRepository.findByClubIdAndUserId(club.getId(), user.getId())
            .orElseThrow(() -> CustomException.of(FORBIDDEN_CLUB_RECRUITMENT_CREATE));

        validateClubManager(clubMember);

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
}
