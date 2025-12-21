package gg.agit.konect.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CLUB_MEMBER_ACCESS;
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

import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMembersResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.dto.JoinedClubsResponse;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubRecruitment;
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
    private final ClubApplyQuestionRepository clubApplyQuestionRepository;
    private final ClubApplyAnswerRepository clubApplyAnswerRepository;
    private final UserRepository userRepository;

    public ClubsResponse getClubs(Integer page, Integer limit, String query, Boolean isRecruiting, Integer userId) {
        User user = userRepository.getById(userId);
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ClubSummaryInfo> clubSummaryInfoPage = clubQueryRepository.findAllByFilter(pageable, query, isRecruiting, user.getUniversity().getId());
        return ClubsResponse.of(clubSummaryInfoPage);
    }

    public ClubDetailResponse getClubDetail(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);
        List<ClubMember> clubMembers = clubMemberRepository.findAllByClubId(club.getId());
        List<ClubMember> clubPresidents = clubMembers.stream()
            .filter(ClubMember::isPresident)
            .toList();
        Integer memberCount = clubMembers.size();
        ClubRecruitment recruitment = clubRecruitmentRepository.findByClubId(clubId).orElse(null);

        boolean isMember = clubMembers.stream()
            .anyMatch(clubMember -> clubMember.getUser().getId().equals(userId));
        Boolean isApplied = isMember || clubApplyRepository.existsByClubIdAndUserId(clubId, userId);

        return ClubDetailResponse.of(club, memberCount, recruitment, clubPresidents, isMember, isApplied);
    }

    public JoinedClubsResponse getJoinedClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserId(userId);
        return JoinedClubsResponse.of(clubMembers);
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

        if (!clubApplyRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_FEE_INFO);
        }

        return ClubFeeInfoResponse.from(club);
    }

    public ClubApplyQuestionsResponse getApplyQuestions(Integer clubId, Integer userId) {
        User user = userRepository.getById(userId);
        List<ClubApplyQuestion> questions = clubApplyQuestionRepository.findAllByClubId(clubId);
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
            throw CustomException.of(ApiResponseCode.ALREADY_APPLIED_CLUB);
        }

        List<ClubApplyQuestion> questions = clubApplyQuestionRepository.findAllByClubId(clubId);
        validateApplyAnswers(questions, request.answers());

        ClubApply apply = clubApplyRepository.save(
            ClubApply.builder()
                .club(club)
                .user(user)
                .build()
        );

        if (!request.answers().isEmpty()) {
            List<ClubApplyAnswer> applyAnswers = request.answers().stream()
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

    private void validateApplyAnswers(List<ClubApplyQuestion> questions, List<ClubApplyRequest.AnswerRequest> answers) {
        Map<Integer, ClubApplyQuestion> questionMap = questions.stream()
            .collect(Collectors.toMap(ClubApplyQuestion::getId, question -> question));

        Set<Integer> answeredQuestionIds = new HashSet<>();
        Set<Integer> seenQuestionIds = new HashSet<>();

        for (ClubApplyRequest.AnswerRequest answer : answers) {
            if (!questionMap.containsKey(answer.questionId())) {
                throw CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_APPLY_QUESTION);
            }

            if (!seenQuestionIds.add(answer.questionId())) {
                throw CustomException.of(ApiResponseCode.DUPLICATE_CLUB_APPLY_QUESTION);
            }

            ClubApplyQuestion question = questionMap.get(answer.questionId());
            boolean hasAnswer = StringUtils.hasText(answer.answer());

            if (question.getIsRequired().equals(TRUE) && !hasAnswer) {
                throw CustomException.of(ApiResponseCode.REQUIRED_CLUB_APPLY_ANSWER_MISSING);
            }

            if (hasAnswer) {
                answeredQuestionIds.add(answer.questionId());
            }
        }

        for (ClubApplyQuestion question : questions) {
            if (question.getIsRequired().equals(TRUE) && !answeredQuestionIds.contains(question.getId())) {
                throw CustomException.of(ApiResponseCode.REQUIRED_CLUB_APPLY_ANSWER_MISSING);
            }
        }
    }

    private ClubApplyQuestion getQuestionById(List<ClubApplyQuestion> questions, Integer questionId) {
        return questions.stream()
            .filter(question -> question.getId().equals(questionId))
            .findFirst()
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_APPLY_QUESTION));
    }
}
