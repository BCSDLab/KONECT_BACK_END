package gg.agit.konect.unit.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_APPLIED_CLUB;
import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_CLUB_MEMBER;
import static gg.agit.konect.global.code.ApiResponseCode.DUPLICATE_CLUB_APPLY_QUESTION;
import static gg.agit.konect.global.code.ApiResponseCode.FEE_PAYMENT_IMAGE_REQUIRED;
import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_PROCESSED_CLUB_APPLY;
import static gg.agit.konect.global.code.ApiResponseCode.INVALID_REQUEST_BODY;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_APPLY_QUESTION;
import static gg.agit.konect.global.code.ApiResponseCode.REQUIRED_CLUB_APPLY_ANSWER_MISSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.bank.model.Bank;
import gg.agit.konect.domain.bank.repository.BankRepository;
import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.dto.ClubApplicationAnswersResponse;
import gg.agit.konect.domain.club.dto.ClubApplicationCondition;
import gg.agit.konect.domain.club.dto.ClubApplicationsResponse;
import gg.agit.konect.domain.club.dto.ClubAppliedClubsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsResponse;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoResponse;
import gg.agit.konect.domain.club.dto.ClubMemberApplicationAnswersResponse;
import gg.agit.konect.domain.club.enums.ClubApplyStatus;
import gg.agit.konect.domain.club.event.ClubApplicationApprovedEvent;
import gg.agit.konect.domain.club.event.ClubApplicationRejectedEvent;
import gg.agit.konect.domain.club.event.ClubApplicationSubmittedEvent;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.repository.ClubApplyAnswerRepository;
import gg.agit.konect.domain.club.repository.ClubApplyQueryRepository;
import gg.agit.konect.domain.club.repository.ClubApplyQuestionRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubApplicationService;
import gg.agit.konect.domain.club.service.ClubPermissionValidator;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ClubApplicationServiceTest extends ServiceTestSupport {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubRecruitmentRepository clubRecruitmentRepository;

    @Mock
    private ClubApplyRepository clubApplyRepository;

    @Mock
    private ClubApplyQuestionRepository clubApplyQuestionRepository;

    @Mock
    private ClubApplyAnswerRepository clubApplyAnswerRepository;

    @Mock
    private ClubApplyQueryRepository clubApplyQueryRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankRepository bankRepository;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ChatRoomMembershipService chatRoomMembershipService;

    @InjectMocks
    private ClubApplicationService clubApplicationService;

    @Test
    @DisplayName("applyClub은 이미 동아리 멤버인 사용자의 지원을 거부한다")
    void applyClubRejectsAlreadyMember() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyRequest request = new ClubApplyRequest(List.of(), null);
        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(true);

        // when & then
        assertErrorCode(() -> clubApplicationService.applyClub(1, 10, request), ALREADY_CLUB_MEMBER);
        verify(clubApplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyClub은 이미 pending 지원서가 있으면 중복 지원을 거부한다")
    void applyClubRejectsAlreadyApplied() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyRequest request = new ClubApplyRequest(List.of(), null);
        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(true);

        // when & then
        assertErrorCode(() -> clubApplicationService.applyClub(1, 10, request), ALREADY_APPLIED_CLUB);
        verify(clubApplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyClub은 회비 필수 동아리에서 납부 이미지가 없으면 실패한다")
    void applyClubRequiresFeePaymentImage() {
        // given
        Club club = createFeeRequiredClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyRequest request = new ClubApplyRequest(List.of(), null);
        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);

        // when & then
        assertErrorCode(() -> clubApplicationService.applyClub(1, 10, request), FEE_PAYMENT_IMAGE_REQUIRED);
        verify(clubApplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyClub은 답변을 저장하고 운영진이 있으면 제출 이벤트를 발행한다")
    void applyClubSavesAnswersAndPublishesSubmittedEvent() {
        // given
        Club club = createClubWithFeeInfo(1, "국민은행");
        User applicant = createUser(10, "2021136001", "지원자");
        User managerUser = createUser(20, "2021136002", "운영진");
        ClubMember manager = ClubMemberFixture.createManager(club, managerUser);
        ClubApplyQuestion question = createQuestion(club, 100, "지원 동기", true, 1, at(2026, 4, 1, 12, 0));
        ClubApplyRequest request = new ClubApplyRequest(
            List.of(new ClubApplyRequest.InnerClubQuestionAnswer(100, "백엔드를 공부하고 싶습니다.")),
            "https://example.com/payment.png"
        );
        Bank bank = org.mockito.Mockito.mock(Bank.class);
        ClubApply savedApply = ClubApply.of(club, applicant, request.feePaymentImageUrl());
        setId(savedApply, 300);

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(applicant);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(List.of(question));
        given(clubApplyRepository.save(any(ClubApply.class))).willReturn(savedApply);
        given(clubMemberRepository.findAllByClubIdAndPositionIn(1,
            gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS))
            .willReturn(List.of(manager));
        given(bankRepository.getByName("국민은행")).willReturn(bank);
        given(bank.getId()).willReturn(7);

        // when
        ClubFeeInfoResponse response = clubApplicationService.applyClub(1, 10, request);

        // then
        verify(clubApplyAnswerRepository).saveAll(argThat(answers -> {
            List<ClubApplyAnswer> savedAnswers = (List<ClubApplyAnswer>)answers;
            return savedAnswers.size() == 1
                && savedAnswers.get(0).getQuestion().getId().equals(100)
                && savedAnswers.get(0).getAnswer().equals("백엔드를 공부하고 싶습니다.");
        }));
        verify(applicationEventPublisher).publishEvent(ClubApplicationSubmittedEvent.of(
            List.of(20),
            300,
            1,
            club.getName(),
            applicant.getName()
        ));
        assertThat(response.bankId()).isEqualTo(7);
        assertThat(response.bankName()).isEqualTo("국민은행");
        assertThat(response.accountNumber()).isEqualTo("123-456-7890");
    }

    @Test
    @DisplayName("approveClubApplication은 멤버 저장, 채팅 멤버십 추가, 승인 이벤트 발행을 수행한다")
    void approveClubApplicationAddsMembershipAndPublishesEvent() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        ClubApply clubApply = ClubApply.of(club, applicant, null);
        ClubMember savedMember = ClubMemberFixture.createMember(club, applicant);

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyRepository.getByIdAndClubIdForUpdate(100, 1)).willReturn(clubApply);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubMemberRepository.save(any(ClubMember.class))).willReturn(savedMember);

        // when
        clubApplicationService.approveClubApplication(1, 100, 99);

        // then
        assertThat(clubApply.getStatus()).isEqualTo(ClubApplyStatus.APPROVED);
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        verify(chatRoomMembershipService).addClubMember(savedMember);
        verify(applicationEventPublisher).publishEvent(ClubApplicationApprovedEvent.of(10, 1, club.getName()));
    }

    @Test
    @DisplayName("approveClubApplication은 이미 멤버인 사용자를 다시 승인하지 않는다")
    void approveClubApplicationRejectsExistingMember() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        ClubApply clubApply = ClubApply.of(club, applicant, null);

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyRepository.getByIdAndClubIdForUpdate(100, 1)).willReturn(clubApply);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(true);

        // when & then
        assertErrorCode(() -> clubApplicationService.approveClubApplication(1, 100, 99), ALREADY_CLUB_MEMBER);
        assertThat(clubApply.getStatus()).isEqualTo(ClubApplyStatus.PENDING);
        verify(chatRoomMembershipService, never()).addClubMember(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("rejectClubApplication은 상태를 거절로 바꾸고 거절 이벤트를 발행한다")
    void rejectClubApplicationChangesStatusAndPublishesEvent() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        ClubApply clubApply = ClubApply.of(club, applicant, null);

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyRepository.getByIdAndClubIdForUpdate(100, 1)).willReturn(clubApply);

        // when
        clubApplicationService.rejectClubApplication(1, 100, 99);

        // then
        assertThat(clubApply.getStatus()).isEqualTo(ClubApplyStatus.REJECTED);
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        verify(applicationEventPublisher).publishEvent(ClubApplicationRejectedEvent.of(10, 1, club.getName()));
    }

    @Test
    @DisplayName("replaceApplyQuestions는 수정된 질문을 soft delete하고 새 질문을 생성하며 빠진 질문도 soft delete한다")
    void replaceApplyQuestionsSoftDeletesAndCreatesQuestions() {
        // given
        Club club = createClub(1);
        ClubApplyQuestion unchangedQuestion = createQuestion(club, 1, "지원 동기", true, 1, at(2026, 4, 1, 10, 0));
        ClubApplyQuestion changedQuestion = createQuestion(club, 2, "관심 분야", false, 2, at(2026, 4, 1, 10, 0));
        ClubApplyQuestion removedQuestion = createQuestion(club, 3, "자기소개", true, 3, at(2026, 4, 1, 10, 0));
        ClubApplyQuestion createdQuestion = createQuestion(club, 4, "새 질문", true, 2, at(2026, 4, 2, 10, 0));
        ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(1, "지원 동기", true),
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(2, "관심 기술", false),
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(null, "새 질문", true)
        ));
        List<ClubApplyQuestion> existingQuestions = List.of(unchangedQuestion, changedQuestion, removedQuestion);
        ArgumentCaptor<List<ClubApplyQuestion>> questionsCaptor = ArgumentCaptor.forClass(List.class);

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1))
            .willReturn(existingQuestions)
            .willReturn(List.of(unchangedQuestion, createdQuestion));

        // when
        var response = clubApplicationService.replaceApplyQuestions(1, 99, request);

        // then
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        verify(clubApplyQuestionRepository).saveAll(questionsCaptor.capture());
        List<ClubApplyQuestion> createdQuestions = questionsCaptor.getValue();
        assertThat(createdQuestions).hasSize(2);
        assertThat(createdQuestions)
            .extracting(ClubApplyQuestion::getQuestion)
            .containsExactly("관심 기술", "새 질문");
        assertThat(unchangedQuestion.getDisplayOrder()).isEqualTo(1);
        assertThat(changedQuestion.getDeletedAt()).isNotNull();
        assertThat(removedQuestion.getDeletedAt()).isNotNull();
        assertThat(response.questions()).hasSize(2);
    }

    @Test
    @DisplayName("replaceApplyQuestions는 중복 questionId를 거부한다")
    void replaceApplyQuestionsRejectsDuplicateQuestionIds() {
        // given
        Club club = createClub(1);
        ClubApplyQuestion existingQuestion = createQuestion(club, 1, "지원 동기", true, 1, at(2026, 4, 1, 10, 0));
        ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(1, "지원 동기", true),
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(1, "지원 동기 수정", true)
        ));

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(
            List.of(existingQuestion));

        // when & then
        assertErrorCode(() -> clubApplicationService.replaceApplyQuestions(1, 99, request),
            DUPLICATE_CLUB_APPLY_QUESTION);
        verify(clubApplyQuestionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("getApprovedMemberApplicationAnswersList는 승인 지원서가 없으면 빈 응답을 즉시 반환한다")
    void getApprovedMemberApplicationAnswersListReturnsEmptyImmediately() {
        // given
        Page<ClubApply> emptyPage = new PageImpl<>(List.of());
        ClubApplicationCondition condition = new ClubApplicationCondition(1, 10, null, null);
        given(clubRepository.getById(1)).willReturn(createClub(1));
        given(clubApplyQueryRepository.findApprovedMemberApplicationsByClubId(1, condition)).willReturn(emptyPage);

        // when
        ClubMemberApplicationAnswersResponse response =
            clubApplicationService.getApprovedMemberApplicationAnswersList(1, 99, condition);

        // then
        assertThat(response.applications()).isEmpty();
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        verify(clubApplyAnswerRepository, never()).findAllByApplyIdsWithQuestion(any());
        verify(clubApplyQuestionRepository, never()).findAllCandidatesVisibleBetweenApplyTimes(any(), any(), any());
    }

    @Test
    @DisplayName("getApprovedMemberApplicationAnswersList는 지원 시점에 보이던 질문만 각 지원서에 포함한다")
    void getApprovedMemberApplicationAnswersListUsesQuestionVisibilityAtApplyTime() {
        // given
        Club club = createClub(1);
        User firstUser = createUser(10, "2021136001", "첫번째");
        User secondUser = createUser(20, "2021136002", "두번째");
        LocalDateTime firstAppliedAt = at(2026, 4, 2, 10, 0);
        LocalDateTime secondAppliedAt = at(2026, 4, 4, 10, 0);

        ClubApply firstApply = createApprovedApply(club, firstUser, 100, firstAppliedAt);
        ClubApply secondApply = createApprovedApply(club, secondUser, 200, secondAppliedAt);
        Page<ClubApply> page = new PageImpl<>(List.of(firstApply, secondApply));
        ClubApplicationCondition condition = new ClubApplicationCondition(1, 10, null, null);

        ClubApplyQuestion oldQuestion = createQuestion(club, 1, "공통 질문", true, 1, at(2026, 4, 1, 9, 0));
        ClubApplyQuestion deletedQuestion = createQuestion(club, 2, "이전 질문", false, 2, at(2026, 4, 1, 9, 0));
        deletedQuestion.softDelete(at(2026, 4, 3, 0, 0));
        ClubApplyQuestion newQuestion = createQuestion(club, 3, "신규 질문", true, 3, at(2026, 4, 3, 12, 0));

        ClubApplyAnswer firstAnswer1 = ClubApplyAnswer.of(firstApply, oldQuestion, "첫번째 공통 답변");
        ClubApplyAnswer firstAnswer2 = ClubApplyAnswer.of(firstApply, deletedQuestion, "첫번째 이전 답변");
        ClubApplyAnswer secondAnswer1 = ClubApplyAnswer.of(secondApply, oldQuestion, "두번째 공통 답변");
        ClubApplyAnswer secondAnswer2 = ClubApplyAnswer.of(secondApply, newQuestion, "두번째 신규 답변");

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQueryRepository.findApprovedMemberApplicationsByClubId(1, condition)).willReturn(page);
        given(clubApplyAnswerRepository.findAllByApplyIdsWithQuestion(List.of(100, 200)))
            .willReturn(List.of(firstAnswer1, firstAnswer2, secondAnswer1, secondAnswer2));
        given(clubApplyQuestionRepository.findAllCandidatesVisibleBetweenApplyTimes(1, firstAppliedAt, secondAppliedAt))
            .willReturn(List.of(oldQuestion, deletedQuestion, newQuestion));

        // when
        ClubMemberApplicationAnswersResponse response =
            clubApplicationService.getApprovedMemberApplicationAnswersList(1, 99, condition);

        // then
        assertThat(response.applications()).hasSize(2);

        ClubApplicationAnswersResponse firstResponse = response.applications().get(0);
        assertThat(firstResponse.applicationId()).isEqualTo(100);
        assertThat(firstResponse.answers())
            .extracting(ClubApplicationAnswersResponse.ClubApplicationAnswerResponse::question)
            .containsExactly("공통 질문", "이전 질문");

        ClubApplicationAnswersResponse secondResponse = response.applications().get(1);
        assertThat(secondResponse.applicationId()).isEqualTo(200);
        assertThat(secondResponse.answers())
            .extracting(ClubApplicationAnswersResponse.ClubApplicationAnswerResponse::question)
            .containsExactly("공통 질문", "신규 질문");
    }

    @Test
    @DisplayName("applyClub은 필수 질문에 답변이 누락되면 실패한다")
    void applyClubRejectsMissingRequiredAnswer() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyQuestion requiredQuestion = createQuestion(club, 100, "지원 동기", true, 1, at(2026, 4, 1, 12, 0));
        ClubApplyRequest request = new ClubApplyRequest(List.of(), null);

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(
            List.of(requiredQuestion));

        // when & then
        assertErrorCode(() -> clubApplicationService.applyClub(1, 10, request), REQUIRED_CLUB_APPLY_ANSWER_MISSING);
        verify(clubApplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyClub은 존재하지 않는 질문 ID에 답변하면 실패한다")
    void applyClubRejectsAnswerToNonExistentQuestion() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyRequest request = new ClubApplyRequest(
            List.of(new ClubApplyRequest.InnerClubQuestionAnswer(999, "답변")),
            null
        );

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(List.of());

        // when & then
        assertErrorCode(() -> clubApplicationService.applyClub(1, 10, request), NOT_FOUND_CLUB_APPLY_QUESTION);
        verify(clubApplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyClub은 답변에 중복 질문 ID가 있으면 실패한다")
    void applyClubRejectsDuplicateAnswerQuestionIds() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyRequest request = new ClubApplyRequest(
            List.of(
                new ClubApplyRequest.InnerClubQuestionAnswer(100, "첫 답변"),
                new ClubApplyRequest.InnerClubQuestionAnswer(100, "중복 답변")
            ),
            null
        );

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);

        // when & then
        assertErrorCode(() -> clubApplicationService.applyClub(1, 10, request), DUPLICATE_CLUB_APPLY_QUESTION);
        verify(clubApplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyClub은 운영진이 없으면 제출 이벤트를 발행하지 않는다")
    void applyClubDoesNotPublishEventWhenNoManagers() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApply savedApply = ClubApply.of(club, user, null);
        setId(savedApply, 300);
        ClubApplyRequest request = new ClubApplyRequest(List.of(), null);

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(List.of());
        given(clubApplyRepository.save(any(ClubApply.class))).willReturn(savedApply);
        given(clubMemberRepository.findAllByClubIdAndPositionIn(1,
            gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS))
            .willReturn(List.of());

        // when
        clubApplicationService.applyClub(1, 10, request);

        // then
        verify(applicationEventPublisher, never()).publishEvent(any());
        verify(clubApplyAnswerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("applyClub은 회비 불필요 동아리에서 이미지 없이도 성공한다")
    void applyClubSucceedsWithoutFeeImage() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApply savedApply = ClubApply.of(club, user, null);
        setId(savedApply, 300);
        ClubApplyRequest request = new ClubApplyRequest(List.of(), null);

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(List.of());
        given(clubApplyRepository.save(any(ClubApply.class))).willReturn(savedApply);
        given(clubMemberRepository.findAllByClubIdAndPositionIn(1,
            gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS))
            .willReturn(List.of());

        // when
        ClubFeeInfoResponse response = clubApplicationService.applyClub(1, 10, request);

        // then
        assertThat(response.bankId()).isNull();
        assertThat(response.bankName()).isNull();
        assertThat(response.amount()).isNull();
    }

    @Test
    @DisplayName("applyClub은 선택 질문에 빈 답변을 허용한다")
    void applyClubAllowsEmptyAnswerForOptionalQuestion() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyQuestion optionalQuestion = createQuestion(club, 100, "관심 분야", false, 1, at(2026, 4, 1, 12, 0));
        ClubApply savedApply = ClubApply.of(club, user, null);
        setId(savedApply, 300);
        ClubApplyRequest request = new ClubApplyRequest(
            List.of(new ClubApplyRequest.InnerClubQuestionAnswer(100, "")),
            null
        );

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(
            List.of(optionalQuestion));
        given(clubApplyRepository.save(any(ClubApply.class))).willReturn(savedApply);
        given(clubMemberRepository.findAllByClubIdAndPositionIn(1,
            gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS))
            .willReturn(List.of());

        // when
        clubApplicationService.applyClub(1, 10, request);

        // then
        verify(clubApplyAnswerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("replaceApplyQuestions는 존재하지 않는 질문 ID를 거부한다")
    void replaceApplyQuestionsRejectsNonExistentQuestionId() {
        // given
        Club club = createClub(1);
        ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(999, "수정된 질문", true)
        ));

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(List.of());

        // when & then
        assertErrorCode(() -> clubApplicationService.replaceApplyQuestions(1, 99, request),
            NOT_FOUND_CLUB_APPLY_QUESTION);
        verify(clubApplyQuestionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("replaceApplyQuestions는 모든 질문이 새로 생성되는 경우 정상 동작한다")
    void replaceApplyQuestionsCreatesAllNewQuestions() {
        // given
        Club club = createClub(1);
        ClubApplyQuestion newQuestion1 = createQuestion(club, 10, "새 질문 1", true, 1, at(2026, 4, 2, 10, 0));
        ClubApplyQuestion newQuestion2 = createQuestion(club, 11, "새 질문 2", false, 2, at(2026, 4, 2, 10, 0));
        ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(null, "새 질문 1", true),
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(null, "새 질문 2", false)
        ));

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1))
            .willReturn(List.of())
            .willReturn(List.of(newQuestion1, newQuestion2));

        // when
        ClubApplyQuestionsResponse response = clubApplicationService.replaceApplyQuestions(1, 99, request);

        // then
        verify(clubApplyQuestionRepository).saveAll(argThat(list -> ((List<?>)list).size() == 2));
        assertThat(response.questions()).hasSize(2);
    }

    @Test
    @DisplayName("replaceApplyQuestions는 빈 질문 목록이면 기존 질문을 모두 soft delete한다")
    void replaceApplyQuestionsSoftDeletesAllWhenEmptyRequest() {
        // given
        Club club = createClub(1);
        ClubApplyQuestion existingQ1 = createQuestion(club, 1, "질문 1", true, 1, at(2026, 4, 1, 10, 0));
        ClubApplyQuestion existingQ2 = createQuestion(club, 2, "질문 2", false, 2, at(2026, 4, 1, 10, 0));
        ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of());

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1))
            .willReturn(List.of(existingQ1, existingQ2))
            .willReturn(List.of());

        // when
        ClubApplyQuestionsResponse response = clubApplicationService.replaceApplyQuestions(1, 99, request);

        // then
        assertThat(existingQ1.getDeletedAt()).isNotNull();
        assertThat(existingQ2.getDeletedAt()).isNotNull();
        verify(clubApplyQuestionRepository, never()).saveAll(any());
        assertThat(response.questions()).isEmpty();
    }

    @Test
    @DisplayName("getClubApplicationAnswers는 특정 지원서의 답변을 질문과 함께 반환한다")
    void getClubApplicationAnswersReturnsAnswersWithQuestions() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        LocalDateTime appliedAt = at(2026, 4, 2, 10, 0);
        ClubApply clubApply = createApprovedApply(club, applicant, 100, appliedAt);
        ClubApplyQuestion question = createQuestion(club, 200, "지원 동기", true, 1, at(2026, 4, 1, 9, 0));
        ClubApplyAnswer answer = ClubApplyAnswer.of(clubApply, question, "성장하고 싶습니다.");

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyRepository.getByIdAndClubId(100, 1)).willReturn(clubApply);
        given(clubApplyAnswerRepository.findAllByApplyIdWithQuestion(100)).willReturn(List.of(answer));
        given(clubApplyQuestionRepository.findAllVisibleAtApplyTime(1, appliedAt)).willReturn(List.of(question));

        // when
        ClubApplicationAnswersResponse response = clubApplicationService.getClubApplicationAnswers(1, 100, 99);

        // then
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        assertThat(response.applicationId()).isEqualTo(100);
        assertThat(response.answers()).hasSize(1);
        assertThat(response.answers().get(0).question()).isEqualTo("지원 동기");
        assertThat(response.answers().get(0).answer()).isEqualTo("성장하고 싶습니다.");
    }

    @Test
    @DisplayName("getApplyQuestions는 삭제되지 않은 질문 목록을 반환한다")
    void getApplyQuestionsReturnsActiveQuestions() {
        // given
        Club club = createClub(1);
        ClubApplyQuestion q1 = createQuestion(club, 1, "지원 동기", true, 1, at(2026, 4, 1, 10, 0));
        ClubApplyQuestion q2 = createQuestion(club, 2, "관심 분야", false, 2, at(2026, 4, 1, 10, 0));

        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(List.of(q1, q2));

        // when
        ClubApplyQuestionsResponse response = clubApplicationService.getApplyQuestions(1, 99);

        // then
        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).question()).isEqualTo("지원 동기");
        assertThat(response.questions().get(1).isRequired()).isFalse();
    }

    @Test
    @DisplayName("getAppliedClubs는 사용자의 대기 중인 지원 목록을 반환한다")
    void getAppliedClubsReturnsPendingApplications() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApply apply = ClubApply.of(club, user, null);
        setId(apply, 100);

        given(clubApplyRepository.findAllPendingByUserIdWithClub(10)).willReturn(List.of(apply));

        // when
        ClubAppliedClubsResponse response = clubApplicationService.getAppliedClubs(10);

        // then
        assertThat(response.appliedClubs()).hasSize(1);
    }

    @Test
    @DisplayName("getAppliedClubs는 대기 중인 지원이 없으면 빈 목록을 반환한다")
    void getAppliedClubsReturnsEmptyWhenNoPendingApplications() {
        // given
        given(clubApplyRepository.findAllPendingByUserIdWithClub(10)).willReturn(List.of());

        // when
        ClubAppliedClubsResponse response = clubApplicationService.getAppliedClubs(10);

        // then
        assertThat(response.appliedClubs()).isEmpty();
    }

    // ========== US-001: applyClub validation boundary cases ==========

    @Test
    @DisplayName("applyClub은 필수 질문에 공백만 있는 답변을 거부한다")
    void applyClubRejectsWhitespaceOnlyRequiredAnswer() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyQuestion requiredQuestion = createQuestion(club, 100, "지원 동기", true, 1, at(2026, 4, 1, 12, 0));
        ClubApplyRequest request = new ClubApplyRequest(
            List.of(new ClubApplyRequest.InnerClubQuestionAnswer(100, "   ")),
            null
        );

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(
            List.of(requiredQuestion));

        // when & then
        assertErrorCode(() -> clubApplicationService.applyClub(1, 10, request), REQUIRED_CLUB_APPLY_ANSWER_MISSING);
        verify(clubApplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyClub은 필수 질문에 null 답변을 거부한다")
    void applyClubRejectsNullAnswerForRequiredQuestion() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        ClubApplyQuestion requiredQuestion = createQuestion(club, 100, "지원 동기", true, 1, at(2026, 4, 1, 12, 0));
        ClubApplyRequest request = new ClubApplyRequest(
            List.of(new ClubApplyRequest.InnerClubQuestionAnswer(100, null)),
            null
        );

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(user);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(
            List.of(requiredQuestion));

        // when & then
        assertErrorCode(() -> clubApplicationService.applyClub(1, 10, request), REQUIRED_CLUB_APPLY_ANSWER_MISSING);
        verify(clubApplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyClub은 여러 운영진의 ID를 모두 제출 이벤트에 포함한다")
    void applyClubPublishesEventWithMultipleManagers() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        User manager1 = createUser(20, "2021136002", "운영진1");
        User manager2 = createUser(21, "2021136003", "운영진2");
        ClubMember member1 = ClubMemberFixture.createManager(club, manager1);
        ClubMember member2 = ClubMemberFixture.createManager(club, manager2);
        ClubApply savedApply = ClubApply.of(club, applicant, null);
        setId(savedApply, 300);
        ClubApplyRequest request = new ClubApplyRequest(List.of(), null);

        given(clubRepository.getById(1)).willReturn(club);
        given(userRepository.getById(10)).willReturn(applicant);
        given(clubMemberRepository.existsByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyRepository.existsPendingByClubIdAndUserId(1, 10)).willReturn(false);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(List.of());
        given(clubApplyRepository.save(any(ClubApply.class))).willReturn(savedApply);
        given(clubMemberRepository.findAllByClubIdAndPositionIn(1,
            gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS))
            .willReturn(List.of(member1, member2));

        // when
        clubApplicationService.applyClub(1, 10, request);

        // then
        verify(applicationEventPublisher).publishEvent(ClubApplicationSubmittedEvent.of(
            List.of(20, 21),
            300,
            1,
            club.getName(),
            applicant.getName()
        ));
    }

    // ========== US-002: Approve/reject logical state transitions ==========

    @Test
    @DisplayName("approveClubApplication은 이미 승인된 지원서를 재승인하면 ALREADY_PROCESSED_CLUB_APPLY를 던진다")
    void approveClubApplicationRejectsAlreadyApproved() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        ClubApply approvedApply = ClubApply.of(club, applicant, null);
        approvedApply.approve();

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyRepository.getByIdAndClubIdForUpdate(100, 1)).willReturn(approvedApply);

        // when & then
        assertErrorCode(() -> clubApplicationService.approveClubApplication(1, 100, 99), ALREADY_PROCESSED_CLUB_APPLY);
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveClubApplication은 이미 거절된 지원서를 승인하면 ALREADY_PROCESSED_CLUB_APPLY를 던진다")
    void approveClubApplicationRejectsAlreadyRejected() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        ClubApply rejectedApply = ClubApply.of(club, applicant, null);
        rejectedApply.reject();

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyRepository.getByIdAndClubIdForUpdate(100, 1)).willReturn(rejectedApply);

        // when & then
        assertErrorCode(() -> clubApplicationService.approveClubApplication(1, 100, 99), ALREADY_PROCESSED_CLUB_APPLY);
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejectClubApplication은 이미 승인된 지원서를 거절하면 ALREADY_PROCESSED_CLUB_APPLY를 던진다")
    void rejectClubApplicationRejectsAlreadyApproved() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        ClubApply approvedApply = ClubApply.of(club, applicant, null);
        approvedApply.approve();

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyRepository.getByIdAndClubIdForUpdate(100, 1)).willReturn(approvedApply);

        // when & then
        assertErrorCode(() -> clubApplicationService.rejectClubApplication(1, 100, 99), ALREADY_PROCESSED_CLUB_APPLY);
        assertThat(approvedApply.getStatus()).isEqualTo(ClubApplyStatus.APPROVED);
    }

    @Test
    @DisplayName("rejectClubApplication은 이미 거절된 지원서를 재거절하면 ALREADY_PROCESSED_CLUB_APPLY를 던진다")
    void rejectClubApplicationRejectsAlreadyRejected() {
        // given
        Club club = createClub(1);
        User applicant = createUser(10, "2021136001", "지원자");
        ClubApply rejectedApply = ClubApply.of(club, applicant, null);
        rejectedApply.reject();

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyRepository.getByIdAndClubIdForUpdate(100, 1)).willReturn(rejectedApply);

        // when & then
        assertErrorCode(() -> clubApplicationService.rejectClubApplication(1, 100, 99), ALREADY_PROCESSED_CLUB_APPLY);
    }

    // ========== US-003: replaceApplyQuestions edge cases ==========

    @Test
    @DisplayName("replaceApplyQuestions는 isRequired가 null이면 기본값 true로 질문을 생성한다")
    void replaceApplyQuestionsDefaultsIsRequiredToTrue() {
        // given
        Club club = createClub(1);
        ClubApplyQuestion createdQuestion = createQuestion(club, 10, "새 질문", true, 1, at(2026, 4, 2, 10, 0));
        ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(null, "새 질문", null)
        ));

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1))
            .willReturn(List.of())
            .willReturn(List.of(createdQuestion));

        // when
        clubApplicationService.replaceApplyQuestions(1, 99, request);

        // then
        verify(clubApplyQuestionRepository).saveAll(argThat(list -> {
            List<ClubApplyQuestion> questions = (List<ClubApplyQuestion>)list;
            return questions.size() == 1 && questions.get(0).getIsRequired().equals(true);
        }));
    }

    @Test
    @DisplayName("replaceApplyQuestions는 내용이 동일하면 displayOrder만 변경하고 soft delete하지 않는다")
    void replaceApplyQuestionsOnlyChangesDisplayOrderWhenContentSame() {
        // given
        Club club = createClub(1);
        ClubApplyQuestion question = createQuestion(club, 1, "지원 동기", true, 2, at(2026, 4, 1, 10, 0));
        // Same content, only repositioning to display order 1
        ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
            new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(1, "지원 동기", true)
        ));

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1))
            .willReturn(List.of(question))
            .willReturn(List.of(question));

        // when
        clubApplicationService.replaceApplyQuestions(1, 99, request);

        // then
        assertThat(question.getDisplayOrder()).isEqualTo(1);
        assertThat(question.getDeletedAt()).isNull();
        verify(clubApplyQuestionRepository, never()).saveAll(any());
    }

    // ========== US-004: Question visibility boundary tests ==========

    @Test
    @DisplayName("createdAt이 appliedAt과 정확히 같은 질문도 가입 시점에 보이는 것으로 처리된다")
    void questionCreatedAtEqualToAppliedAtIsVisible() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        LocalDateTime appliedAt = at(2026, 4, 2, 10, 0);
        ClubApply apply = createApprovedApply(club, user, 100, appliedAt);
        Page<ClubApply> page = new PageImpl<>(List.of(apply));
        ClubApplicationCondition condition = new ClubApplicationCondition(1, 10, null, null);

        ClubApplyQuestion question = createQuestion(club, 1, "질문", true, 1, appliedAt);
        ClubApplyAnswer answer = ClubApplyAnswer.of(apply, question, "답변");

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQueryRepository.findApprovedMemberApplicationsByClubId(1, condition)).willReturn(page);
        given(clubApplyAnswerRepository.findAllByApplyIdsWithQuestion(List.of(100))).willReturn(List.of(answer));
        given(clubApplyQuestionRepository.findAllCandidatesVisibleBetweenApplyTimes(1, appliedAt, appliedAt))
            .willReturn(List.of(question));

        // when
        ClubMemberApplicationAnswersResponse response =
            clubApplicationService.getApprovedMemberApplicationAnswersList(1, 99, condition);

        // then
        assertThat(response.applications()).hasSize(1);
        assertThat(response.applications().get(0).answers()).hasSize(1);
        assertThat(response.applications().get(0).answers().get(0).question()).isEqualTo("질문");
    }

    @Test
    @DisplayName("deletedAt이 appliedAt과 정확히 같은 질문은 가입 시점에 보이지 않는다")
    void questionDeletedAtEqualToAppliedAtIsNotVisible() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        LocalDateTime appliedAt = at(2026, 4, 2, 10, 0);
        ClubApply apply = createApprovedApply(club, user, 100, appliedAt);
        Page<ClubApply> page = new PageImpl<>(List.of(apply));
        ClubApplicationCondition condition = new ClubApplicationCondition(1, 10, null, null);

        ClubApplyQuestion question = createQuestion(club, 1, "삭제된 질문", true, 1, at(2026, 4, 1, 9, 0));
        question.softDelete(appliedAt);

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQueryRepository.findApprovedMemberApplicationsByClubId(1, condition)).willReturn(page);
        given(clubApplyAnswerRepository.findAllByApplyIdsWithQuestion(List.of(100))).willReturn(List.of());
        given(clubApplyQuestionRepository.findAllCandidatesVisibleBetweenApplyTimes(1, appliedAt, appliedAt))
            .willReturn(List.of(question));

        // when
        ClubMemberApplicationAnswersResponse response =
            clubApplicationService.getApprovedMemberApplicationAnswersList(1, 99, condition);

        // then - isAfter returns false for equal timestamps, so question is filtered out
        assertThat(response.applications()).hasSize(1);
        assertThat(response.applications().get(0).answers()).isEmpty();
    }

    @Test
    @DisplayName("getApprovedMemberApplicationAnswersList는 지원서가 하나뿐이어도 정상 동작한다")
    void getApprovedMemberApplicationAnswersListWithSingleApplication() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "지원자");
        LocalDateTime appliedAt = at(2026, 4, 2, 10, 0);
        ClubApply apply = createApprovedApply(club, user, 100, appliedAt);
        Page<ClubApply> page = new PageImpl<>(List.of(apply));
        ClubApplicationCondition condition = new ClubApplicationCondition(1, 10, null, null);

        ClubApplyQuestion question = createQuestion(club, 1, "질문", true, 1, at(2026, 4, 1, 9, 0));
        ClubApplyAnswer answer = ClubApplyAnswer.of(apply, question, "답변");

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQueryRepository.findApprovedMemberApplicationsByClubId(1, condition)).willReturn(page);
        given(clubApplyAnswerRepository.findAllByApplyIdsWithQuestion(List.of(100))).willReturn(List.of(answer));
        given(clubApplyQuestionRepository.findAllCandidatesVisibleBetweenApplyTimes(1, appliedAt, appliedAt))
            .willReturn(List.of(question));

        // when
        ClubMemberApplicationAnswersResponse response =
            clubApplicationService.getApprovedMemberApplicationAnswersList(1, 99, condition);

        // then - minAppliedAt == maxAppliedAt
        assertThat(response.applications()).hasSize(1);
        assertThat(response.applications().get(0).applicationId()).isEqualTo(100);
    }

    // ========== US-005: Untested service method coverage ==========

    @Test
    @DisplayName("getApprovedMemberApplicationAnswers는 특정 회원의 최신 승인 지원서를 반환한다")
    void getApprovedMemberApplicationAnswersReturnsLatestApproved() {
        // given
        Club club = createClub(1);
        User targetUser = createUser(10, "2021136001", "회원");
        ClubMember targetMember = ClubMemberFixture.createMember(club, targetUser);
        LocalDateTime appliedAt = at(2026, 4, 2, 10, 0);
        ClubApply clubApply = createApprovedApply(club, targetUser, 100, appliedAt);
        ClubApplyQuestion question = createQuestion(club, 200, "지원 동기", true, 1, at(2026, 4, 1, 9, 0));
        ClubApplyAnswer answer = ClubApplyAnswer.of(clubApply, question, "성장하고 싶습니다.");

        given(clubRepository.getById(1)).willReturn(club);
        given(clubMemberRepository.getByClubIdAndUserId(1, 10)).willReturn(targetMember);
        given(clubApplyRepository.getLatestApprovedByClubIdAndUserId(1, 10)).willReturn(clubApply);
        given(clubApplyAnswerRepository.findAllByApplyIdWithQuestion(100)).willReturn(List.of(answer));
        given(clubApplyQuestionRepository.findAllVisibleAtApplyTime(1, appliedAt)).willReturn(List.of(question));

        // when
        ClubApplicationAnswersResponse response =
            clubApplicationService.getApprovedMemberApplicationAnswers(1, 10, 99);

        // then
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        assertThat(response.applicationId()).isEqualTo(100);
        assertThat(response.answers()).hasSize(1);
    }

    @Test
    @DisplayName("getClubApplications은 상시 모집 동아리의 전체 지원서를 조회한다")
    void getClubApplicationsWithAlwaysRecruiting() {
        // given
        Club club = createClub(1);
        ClubRecruitment recruitment = ClubRecruitment.of(null, null, true, "상시 모집", club);
        ClubApplicationCondition condition = new ClubApplicationCondition(1, 10, null, null);
        Page<ClubApply> page = new PageImpl<>(List.of());

        given(clubRepository.getById(1)).willReturn(club);
        given(clubRecruitmentRepository.getByClubId(1)).willReturn(recruitment);
        given(clubApplyQueryRepository.findAllByClubId(1, condition)).willReturn(page);

        // when
        ClubApplicationsResponse response = clubApplicationService.getClubApplications(1, 99, condition);

        // then
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        verify(clubApplyQueryRepository).findAllByClubId(1, condition);
        verify(clubApplyQueryRepository, never()).findAllByClubIdAndCreatedAtBetween(any(), any(), any(), any());
        assertThat(response.applications()).isEmpty();
    }

    @Test
    @DisplayName("getClubApplications은 기간 모집 동아리의 기간 내 지원서만 조회한다")
    void getClubApplicationsWithPeriodBasedRecruitment() {
        // given
        Club club = createClub(1);
        LocalDateTime startAt = at(2026, 4, 1, 0, 0);
        LocalDateTime endAt = at(2026, 4, 30, 23, 59);
        ClubRecruitment recruitment = ClubRecruitment.of(startAt, endAt, false, "4월 모집", club);
        ClubApplicationCondition condition = new ClubApplicationCondition(1, 10, null, null);
        Page<ClubApply> page = new PageImpl<>(List.of());

        given(clubRepository.getById(1)).willReturn(club);
        given(clubRecruitmentRepository.getByClubId(1)).willReturn(recruitment);
        given(clubApplyQueryRepository.findAllByClubIdAndCreatedAtBetween(1, startAt, endAt, condition)).willReturn(
            page);

        // when
        ClubApplicationsResponse response = clubApplicationService.getClubApplications(1, 99, condition);

        // then
        verify(clubApplyQueryRepository).findAllByClubIdAndCreatedAtBetween(1, startAt, endAt, condition);
        verify(clubApplyQueryRepository, never()).findAllByClubId(any(), any());
        assertThat(response.applications()).isEmpty();
    }

    @Test
    @DisplayName("getApprovedMemberApplications은 승인된 회원 지원서를 페이지네이션하여 반환한다")
    void getApprovedMemberApplicationsReturnsPagedResults() {
        // given
        Club club = createClub(1);
        User user = createUser(10, "2021136001", "회원");
        ClubApply apply = createApprovedApply(club, user, 100, at(2026, 4, 2, 10, 0));
        ClubApplicationCondition condition = new ClubApplicationCondition(1, 10, null, null);
        Page<ClubApply> page = new PageImpl<>(List.of(apply));

        given(clubRepository.getById(1)).willReturn(club);
        given(clubApplyQueryRepository.findApprovedMemberApplicationsByClubId(1, condition)).willReturn(page);

        // when
        ClubApplicationsResponse response =
            clubApplicationService.getApprovedMemberApplications(1, 99, condition);

        // then
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        assertThat(response.applications()).hasSize(1);
        assertThat(response.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getFeeInfo는 동아리 회비 정보를 반환한다")
    void getFeeInfoReturnsClubFeeInfo() {
        // given
        Club club = createClubWithFeeInfo(1, "국민은행");
        Bank bank = org.mockito.Mockito.mock(Bank.class);

        given(clubRepository.getById(1)).willReturn(club);
        given(bankRepository.getByName("국민은행")).willReturn(bank);
        given(bank.getId()).willReturn(7);

        // when
        ClubFeeInfoResponse response = clubApplicationService.getFeeInfo(1);

        // then
        assertThat(response.bankId()).isEqualTo(7);
        assertThat(response.bankName()).isEqualTo("국민은행");
        assertThat(response.accountNumber()).isEqualTo("123-456-7890");
        assertThat(response.accountHolder()).isEqualTo("BCSD");
    }

    @Test
    @DisplayName("getFeeInfo는 회비 은행 정보가 없으면 bankRepository를 호출하지 않고 null을 반환한다")
    void getFeeInfoReturnsNullBankWhenNoFeeInfo() {
        // given
        Club club = createClub(1);

        given(clubRepository.getById(1)).willReturn(club);

        // when
        ClubFeeInfoResponse response = clubApplicationService.getFeeInfo(1);

        // then
        assertThat(response.bankId()).isNull();
        assertThat(response.bankName()).isNull();
        verify(bankRepository, never()).getByName(any());
    }

    @Test
    @DisplayName("replaceFeeInfo는 동아리 회비 정보를 업데이트하고 응답을 반환한다")
    void replaceFeeInfoUpdatesClubFeeInfo() {
        // given
        Club club = createClub(1);
        User managerUser = createUser(99, "2021136000", "운영진");
        Bank newBank = org.mockito.Mockito.mock(Bank.class);
        ClubFeeInfoReplaceRequest request = new ClubFeeInfoReplaceRequest("5만원", 3, "987-654-3210", "BCSD");

        given(userRepository.getById(99)).willReturn(managerUser);
        given(clubRepository.getById(1)).willReturn(club);
        given(bankRepository.getById(3)).willReturn(newBank);
        given(newBank.getName()).willReturn("신한은행");

        // when
        ClubFeeInfoResponse response = clubApplicationService.replaceFeeInfo(1, 99, request);

        // then
        verify(clubPermissionValidator).validateManagerAccess(1, 99);
        assertThat(response.amount()).isEqualTo("5만원");
        assertThat(response.bankId()).isEqualTo(3);
        assertThat(response.bankName()).isEqualTo("신한은행");
        assertThat(response.accountNumber()).isEqualTo("987-654-3210");
        assertThat(response.accountHolder()).isEqualTo("BCSD");
    }

    @Test
    @DisplayName("replaceFeeInfo는 회비 정보가 일부만 입력되면 INVALID_REQUEST_BODY를 던진다")
    void replaceFeeInfoRejectsPartialFeeInfo() {
        // given
        Club club = createClub(1);
        User managerUser = createUser(99, "2021136000", "운영진");
        Bank bank = org.mockito.Mockito.mock(Bank.class);
        ClubFeeInfoReplaceRequest request = new ClubFeeInfoReplaceRequest("5만원", 3, null, null);

        given(userRepository.getById(99)).willReturn(managerUser);
        given(clubRepository.getById(1)).willReturn(club);
        given(bankRepository.getById(3)).willReturn(bank);
        given(bank.getName()).willReturn("신한은행");

        // when & then
        assertErrorCode(() -> clubApplicationService.replaceFeeInfo(1, 99, request), INVALID_REQUEST_BODY);
    }

    private Club createClub(Integer clubId) {
        return ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId);
    }

    private Club createFeeRequiredClub(Integer clubId) {
        Club club = createClub(clubId);
        club.updateSettings(null, null, true);
        return club;
    }

    private Club createClubWithFeeInfo(Integer clubId, String bankName) {
        Club club = createFeeRequiredClub(clubId);
        club.replaceFeeInfo("30000", bankName, "123-456-7890", "BCSD");
        return club;
    }

    private User createUser(Integer id, String studentNumber, String name) {
        return UserFixture.createUserWithId(
            UniversityFixture.createWithId(1),
            id,
            name,
            studentNumber,
            gg.agit.konect.domain.user.enums.UserRole.USER
        );
    }

    private ClubApplyQuestion createQuestion(
        Club club,
        Integer id,
        String question,
        boolean isRequired,
        int displayOrder,
        LocalDateTime createdAt
    ) {
        ClubApplyQuestion applyQuestion = ClubApplyQuestion.of(club, question, isRequired, displayOrder);
        setId(applyQuestion, id);
        setCreatedAt(applyQuestion, createdAt);
        return applyQuestion;
    }

    private ClubApply createApprovedApply(Club club, User user, Integer id, LocalDateTime createdAt) {
        ClubApply apply = ClubApply.of(club, user, null);
        setId(apply, id);
        setCreatedAt(apply, createdAt);
        apply.approve();
        return apply;
    }

    private void setId(Object target, Integer id) {
        ReflectionTestUtils.setField(target, "id", id);
    }

    private void setCreatedAt(Object target, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(target, "createdAt", createdAt);
        ReflectionTestUtils.setField(target, "updatedAt", createdAt);
    }

    private LocalDateTime at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    private void assertErrorCode(ThrowingCallable callable, ApiResponseCode errorCode) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(errorCode));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
