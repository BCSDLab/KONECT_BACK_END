package gg.agit.konect.domain.club.service;

import static java.lang.Boolean.TRUE;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.ClubSurveyQuestionsResponse;
import gg.agit.konect.domain.club.dto.JoinedClubsResponse;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import gg.agit.konect.domain.club.model.ClubSurveyQuestion;
import gg.agit.konect.domain.club.repository.ClubApplyAnswerRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubQueryRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.repository.ClubSurveyQuestionRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
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
    private final ClubSurveyQuestionRepository clubSurveyQuestionRepository;
    private final ClubApplyAnswerRepository clubApplyAnswerRepository;
    private final UserRepository userRepository;

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

    public ClubFeeInfoResponse getFeeInfo(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);

        if (!clubApplyRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_FEE_INFO);
        }

        return ClubFeeInfoResponse.from(club);
    }

    public ClubSurveyQuestionsResponse getSurveyQuestions(Integer clubId) {
        List<ClubSurveyQuestion> questions = clubSurveyQuestionRepository.findAllByClubId(clubId);
        return ClubSurveyQuestionsResponse.from(questions);
    }

    @Transactional
    public ClubFeeInfoResponse applyClub(Integer clubId, Integer userId, ClubApplyRequest request) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);

        if (clubApplyRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ApiResponseCode.ALREADY_APPLIED_CLUB);
        }

        List<ClubSurveyQuestion> questions = clubSurveyQuestionRepository.findAllByClubId(clubId);
        List<ClubApplyRequest.AnswerRequest> answers = (request == null || request.answers() == null)
            ? List.of()
            : request.answers();
        validateSurveyAnswers(questions, answers);

        ClubApply apply = clubApplyRepository.save(
            ClubApply.builder()
                .club(club)
                .user(user)
                .build()
        );

        if (!answers.isEmpty()) {
            List<ClubApplyAnswer> applyAnswers = answers.stream()
                .filter(answer -> StringUtils.hasText(answer.answer()))
                .map(answer -> ClubApplyAnswer.builder()
                    .apply(apply)
                    .question(getQuestionById(questions, answer.questionId()))
                    .answer(answer.answer())
                    .build()
                ).toList();

            if (!applyAnswers.isEmpty()) {
                clubApplyAnswerRepository.saveAll(applyAnswers);
            }
        }

        return ClubFeeInfoResponse.from(club);
    }

    private void validateSurveyAnswers(List<ClubSurveyQuestion> questions, List<ClubApplyRequest.AnswerRequest> answers) {
        Map<Integer, ClubSurveyQuestion> questionMap = questions.stream()
            .collect(Collectors.toMap(ClubSurveyQuestion::getId, question -> question));

        Set<Integer> answeredQuestionIds = new HashSet<>();
        Set<Integer> seenQuestionIds = new HashSet<>();

        for (ClubApplyRequest.AnswerRequest answer : answers) {
            if (!questionMap.containsKey(answer.questionId())) {
                throw CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_SURVEY_QUESTION);
            }

            if (!seenQuestionIds.add(answer.questionId())) {
                throw CustomException.of(ApiResponseCode.DUPLICATE_CLUB_SURVEY_QUESTION);
            }

            ClubSurveyQuestion question = questionMap.get(answer.questionId());
            boolean hasAnswer = StringUtils.hasText(answer.answer());

            if (question.getIsRequired().equals(TRUE) && !hasAnswer) {
                throw CustomException.of(ApiResponseCode.REQUIRED_CLUB_SURVEY_ANSWER_MISSING);
            }

            if (hasAnswer) {
                answeredQuestionIds.add(answer.questionId());
            }
        }

        for (ClubSurveyQuestion question : questions) {
            if (question.getIsRequired().equals(TRUE) && !answeredQuestionIds.contains(question.getId())) {
                throw CustomException.of(ApiResponseCode.REQUIRED_CLUB_SURVEY_ANSWER_MISSING);
            }
        }
    }

    private ClubSurveyQuestion getQuestionById(List<ClubSurveyQuestion> questions, Integer questionId) {
        return questions.stream()
            .filter(question -> question.getId().equals(questionId))
            .findFirst()
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_SURVEY_QUESTION));
    }
}
