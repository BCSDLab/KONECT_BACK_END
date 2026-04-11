package gg.agit.konect.unit.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_APPLIED_CLUB;
import static gg.agit.konect.global.code.ApiResponseCode.ALREADY_CLUB_MEMBER;
import static gg.agit.konect.global.code.ApiResponseCode.DUPLICATE_CLUB_APPLY_QUESTION;
import static gg.agit.konect.global.code.ApiResponseCode.FEE_PAYMENT_IMAGE_REQUIRED;
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
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
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
        given(clubMemberRepository.findAllByClubIdAndPositionIn(1, gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS))
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
        given(clubApplyRepository.getByIdAndClubId(100, 1)).willReturn(clubApply);
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
        given(clubApplyRepository.getByIdAndClubId(100, 1)).willReturn(clubApply);
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
        given(clubApplyRepository.getByIdAndClubId(100, 1)).willReturn(clubApply);

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
        given(clubApplyQuestionRepository.findAllByClubIdOrderByDisplayOrderAsc(1)).willReturn(List.of(existingQuestion));

        // when & then
        assertErrorCode(() -> clubApplicationService.replaceApplyQuestions(1, 99, request), DUPLICATE_CLUB_APPLY_QUESTION);
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
