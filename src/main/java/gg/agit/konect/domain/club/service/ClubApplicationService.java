package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS;
import static gg.agit.konect.domain.club.enums.ClubPosition.MEMBER;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.bank.repository.BankRepository;
import gg.agit.konect.domain.club.dto.ClubApplicationAnswersResponse;
import gg.agit.konect.domain.club.dto.ClubApplicationCondition;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubAppliedClubsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubApplyQuestionAnswers;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.repository.ClubApplyAnswerRepository;
import gg.agit.konect.domain.club.repository.ClubApplyQueryRepository;
import gg.agit.konect.domain.club.repository.ClubApplyQuestionRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubApplicationService {

    private final ClubRepository clubRepository;
    private final ClubRecruitmentRepository clubRecruitmentRepository;
    private final ClubApplyRepository clubApplyRepository;
    private final ClubApplyQuestionRepository clubApplyQuestionRepository;
    private final ClubApplyAnswerRepository clubApplyAnswerRepository;
    private final ClubApplyQueryRepository clubApplyQueryRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final BankRepository bankRepository;
    private final ClubPermissionValidator clubPermissionValidator;

    public ClubAppliedClubsResponse getAppliedClubs(Integer userId) {
        List<ClubApply> clubApplies = clubApplyRepository.findAllPendingByUserIdWithClub(userId);
        return ClubAppliedClubsResponse.from(clubApplies);
    }

    public ClubApplicationsResponse getClubApplications(
        Integer clubId,
        Integer userId,
        ClubApplicationCondition condition
    ) {
        clubRepository.getById(clubId);

        clubPermissionValidator.validateManagerAccess(clubId, userId);

        ClubRecruitment recruitment = clubRecruitmentRepository.getByClubId(clubId);
        Page<ClubApply> clubAppliesPage = findApplicationsByRecruitmentPeriod(clubId, recruitment, condition);

        return ClubApplicationsResponse.from(clubAppliesPage);
    }

    public ClubApplicationAnswersResponse getClubApplicationAnswers(
        Integer clubId,
        Integer applicationId,
        Integer userId
    ) {
        clubRepository.getById(clubId);

        clubPermissionValidator.validateManagerAccess(clubId, userId);

        ClubApply clubApply = clubApplyRepository.getByIdAndClubId(applicationId, clubId);
        List<ClubApplyQuestion> questions =
            clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(clubId);
        List<ClubApplyAnswer> answers = clubApplyAnswerRepository.findAllByApplyIdWithQuestion(applicationId);

        return ClubApplicationAnswersResponse.of(clubApply, questions, answers);
    }

    @Transactional
    public void approveClubApplication(Integer clubId, Integer applicationId, Integer userId) {
        Club club = clubRepository.getById(clubId);

        clubPermissionValidator.validateLeaderAccess(clubId, userId);

        ClubApply clubApply = clubApplyRepository.getByIdAndClubId(applicationId, clubId);
        User applicant = clubApply.getUser();

        if (clubMemberRepository.existsByClubIdAndUserId(clubId, applicant.getId())) {
            throw CustomException.of(ALREADY_CLUB_MEMBER);
        }

        ClubMember newMember = ClubMember.builder()
            .club(club)
            .user(applicant)
            .clubPosition(MEMBER)
            .isFeePaid(true)
            .build();

        clubMemberRepository.save(newMember);
        clubApplyRepository.delete(clubApply);
    }

    @Transactional
    public void rejectClubApplication(Integer clubId, Integer applicationId, Integer userId) {
        clubRepository.getById(clubId);

        clubPermissionValidator.validateLeaderAccess(clubId, userId);

        ClubApply clubApply = clubApplyRepository.getByIdAndClubId(applicationId, clubId);
        clubApplyRepository.delete(clubApply);
    }

    @Transactional
    public ClubFeeInfoResponse applyClub(Integer clubId, Integer userId, ClubApplyRequest request) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);

        if (clubApplyRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ALREADY_APPLIED_CLUB);
        }

        validateFeePaymentImage(club, request.feePaymentImageUrl());

        List<ClubApplyQuestion> questions =
            clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(clubId);
        ClubApplyQuestionAnswers answers = ClubApplyQuestionAnswers.of(questions, request.toAnswerMap());

        ClubApply apply = clubApplyRepository.save(
            ClubApply.of(club, user, request.feePaymentImageUrl())
        );

        List<ClubApplyAnswer> applyAnswers = answers.toEntities(apply);

        if (!applyAnswers.isEmpty()) {
            clubApplyAnswerRepository.saveAll(applyAnswers);
        }

        return ClubFeeInfoResponse.from(club);
    }

    private void validateFeePaymentImage(Club club, String feePaymentImageUrl) {
        ClubRecruitment recruitment = clubRecruitmentRepository.findByClubId(club.getId())
            .orElse(null);

        if (recruitment != null
            && Boolean.TRUE.equals(recruitment.getIsFeeRequired())
            && !StringUtils.hasText(feePaymentImageUrl)) {
            throw CustomException.of(FEE_PAYMENT_IMAGE_REQUIRED);
        }
    }

    public ClubApplyQuestionsResponse getApplyQuestions(Integer clubId, Integer userId) {
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

        clubPermissionValidator.validateManagerAccess(clubId, userId);

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

    private Page<ClubApply> findApplicationsByRecruitmentPeriod(
        Integer clubId,
        ClubRecruitment recruitment,
        ClubApplicationCondition condition
    ) {
        if (recruitment.getIsAlwaysRecruiting()) {
            return clubApplyQueryRepository.findAllByClubId(clubId, condition);
        }

        LocalDateTime startDateTime = recruitment.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = recruitment.getEndDate().atTime(LocalTime.MAX);

        return clubApplyQueryRepository.findAllByClubIdAndCreatedAtBetween(
            clubId,
            startDateTime,
            endDateTime,
            condition
        );
    }

    public ClubFeeInfoResponse getFeeInfo(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);

        if (!user.isAdmin()) {
            boolean isApplied = clubApplyRepository.existsByClubIdAndUserId(clubId, userId);
            boolean isManager = clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(
                clubId,
                userId,
                MANAGERS
            );

            if (!isApplied && !isManager) {
                throw CustomException.of(FORBIDDEN_CLUB_FEE_INFO);
            }
        }

        return ClubFeeInfoResponse.from(club);
    }

    @Transactional
    public ClubFeeInfoResponse replaceFeeInfo(Integer clubId, Integer userId, ClubFeeInfoReplaceRequest request) {
        userRepository.getById(userId);
        Club club = clubRepository.getById(clubId);

        clubPermissionValidator.validateManagerAccess(clubId, userId);

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
}
