package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import gg.agit.konect.domain.bank.model.Bank;
import gg.agit.konect.domain.bank.repository.BankRepository;
import gg.agit.konect.domain.club.dto.ClubApplicationCondition;
import gg.agit.konect.domain.club.dto.ClubApplyQuestionsReplaceRequest;
import gg.agit.konect.domain.club.dto.ClubApplyRequest;
import gg.agit.konect.domain.club.dto.ClubFeeInfoReplaceRequest;
import gg.agit.konect.domain.club.enums.ClubApplicationSortBy;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.repository.ClubApplyAnswerRepository;
import gg.agit.konect.domain.club.repository.ClubApplyQueryRepository;
import gg.agit.konect.domain.club.repository.ClubApplyQuestionRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubApplicationService 단위 테스트")
class ClubApplicationServiceTest {

    private static final int CLUB_ID = 100;
    private static final int USER_ID = 200;
    private static final int APPLICANT_ID = 201;
    private static final int APPLICATION_ID = 300;
    private static final int QUESTION_ID = 400;
    private static final int UNIVERSITY_ID = 1;
    private static final int PAGE = 1;
    private static final int LIMIT = 10;
    private static final int YEAR = 2026;
    private static final int MONTH_JANUARY = 1;
    private static final int START_DAY = 10;
    private static final int END_DAY = 20;
    private static final int SECOND_QUESTION_ID = 401;
    private static final int BANK_ID = 9;
    private static final int CLUB_FEE_AMOUNT = 10000;
    private static final int UPDATED_FEE_AMOUNT = 5000;

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

    @InjectMocks
    private ClubApplicationService clubApplicationService;

    @Nested
    @DisplayName("applyClub 테스트")
    class ApplyClubTests {

        @Test
        @DisplayName("이미 지원한 경우 ALREADY_APPLIED_CLUB 예외가 발생한다")
        void applyClubWithDuplicatedApplicationThrowsCustomException() {
            // Given
            Club club = createClub(CLUB_ID);
            User user = createUser(USER_ID, "신청자");
            ClubApplyRequest request = new ClubApplyRequest(List.of());
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.applyClub(CLUB_ID, USER_ID, request))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(getErrorCode(ex))
                    .isEqualTo(ApiResponseCode.ALREADY_APPLIED_CLUB));
        }

        @Test
        @DisplayName("필수 질문 답변이 누락된 경우 REQUIRED_CLUB_APPLY_ANSWER_MISSING 예외가 발생한다")
        void applyClubWithMissingRequiredAnswerThrowsCustomException() {
            // Given
            Club club = createClub(CLUB_ID);
            User user = createUser(USER_ID, "신청자");
            ClubApplyQuestion requiredQuestion = ClubApplyQuestion.builder()
                .id(QUESTION_ID)
                .club(club)
                .question("필수 질문")
                .isRequired(true)
                .build();
            ClubApplyRequest request = new ClubApplyRequest(List.of());
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(false);
            when(clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(CLUB_ID))
                .thenReturn(List.of(requiredQuestion));

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.applyClub(CLUB_ID, USER_ID, request))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(getErrorCode(ex))
                    .isEqualTo(ApiResponseCode.REQUIRED_CLUB_APPLY_ANSWER_MISSING));
        }

        @Test
        @DisplayName("정상 지원 시 지원서와 답변을 저장한다")
        void applyClubWithValidRequestSavesApplyAndAnswers() {
            // Given
            Club club = createClub(CLUB_ID);
            User user = createUser(USER_ID, "신청자");
            ClubApplyQuestion question = ClubApplyQuestion.builder()
                .id(QUESTION_ID)
                .club(club)
                .question("자기소개")
                .isRequired(false)
                .build();
            ClubApply savedApply = ClubApply.builder()
                .id(APPLICATION_ID)
                .club(club)
                .user(user)
                .build();
            ClubApplyRequest request = new ClubApplyRequest(
                List.of(new ClubApplyRequest.InnerClubQuestionAnswer(QUESTION_ID, "안녕하세요"))
            );

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(false);
            when(clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(CLUB_ID)).thenReturn(List.of(question));
            when(clubApplyRepository.save(any(ClubApply.class))).thenReturn(savedApply);

            // When
            clubApplicationService.applyClub(CLUB_ID, USER_ID, request);

            // Then
            verify(clubApplyRepository).save(any(ClubApply.class));
            verify(clubApplyAnswerRepository).saveAll(any());
        }
    }

    @Nested
    @DisplayName("getAppliedClubs 테스트")
    class GetAppliedClubsTests {

        @Test
        @DisplayName("대기중 지원 목록을 그대로 응답 DTO로 반환한다")
        void getAppliedClubsReturnsPendingApplicationList() {
            // Given
            Club club = createClub(CLUB_ID);
            User user = createUser(USER_ID, "신청자");
            ClubApply apply = ClubApply.builder().id(APPLICATION_ID).club(club).user(user).build();
            when(clubApplyRepository.findAllPendingByUserIdWithClub(USER_ID)).thenReturn(List.of(apply));

            // When
            var response = clubApplicationService.getAppliedClubs(USER_ID);

            // Then
            assertThat(response.appliedClubs()).hasSize(1);
            assertThat(response.appliedClubs().get(0).id()).isEqualTo(CLUB_ID);
        }

        @Test
        @DisplayName("대기중 지원 목록이 없으면 빈 목록을 반환한다")
        void getAppliedClubsWithEmptyListReturnsEmptyResponse() {
            // Given
            when(clubApplyRepository.findAllPendingByUserIdWithClub(USER_ID)).thenReturn(List.of());

            // When
            var response = clubApplicationService.getAppliedClubs(USER_ID);

            // Then
            assertThat(response.appliedClubs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("approveClubApplication 테스트")
    class ApproveTests {

        @Test
        @DisplayName("이미 멤버인 경우 ALREADY_CLUB_MEMBER 예외가 발생한다")
        void approveClubApplicationWhenAlreadyMemberThrowsCustomException() {
            // Given
            Club club = createClub(CLUB_ID);
            User applicant = createUser(APPLICANT_ID, "지원자");
            ClubApply apply = ClubApply.builder().id(APPLICATION_ID).club(club).user(applicant).build();

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubApplyRepository.getByIdAndClubId(APPLICATION_ID, CLUB_ID)).thenReturn(apply);
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, APPLICANT_ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.approveClubApplication(CLUB_ID, APPLICATION_ID, USER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(getErrorCode(ex))
                    .isEqualTo(ApiResponseCode.ALREADY_CLUB_MEMBER));
        }

        @Test
        @DisplayName("정상 승인 시 멤버를 저장하고 지원서를 삭제한다")
        void approveClubApplicationWithValidRequestSavesMemberAndDeletesApply() {
            // Given
            Club club = createClub(CLUB_ID);
            User applicant = createUser(APPLICANT_ID, "지원자");
            ClubApply apply = ClubApply.builder().id(APPLICATION_ID).club(club).user(applicant).build();

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubApplyRepository.getByIdAndClubId(APPLICATION_ID, CLUB_ID)).thenReturn(apply);
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, APPLICANT_ID)).thenReturn(false);

            // When
            clubApplicationService.approveClubApplication(CLUB_ID, APPLICATION_ID, USER_ID);

            // Then
            verify(clubMemberRepository).save(any());
            verify(clubApplyRepository).delete(apply);
        }

        @Test
        @DisplayName("권한이 없으면 validator 예외를 그대로 전파한다")
        void approveClubApplicationWithoutLeaderAccessPropagatesException() {
            // Given
            Club club = createClub(CLUB_ID);
            CustomException forbidden = CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS);
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            org.mockito.Mockito.doThrow(forbidden)
                .when(clubPermissionValidator)
                .validateLeaderAccess(CLUB_ID, USER_ID);

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.approveClubApplication(CLUB_ID, APPLICATION_ID, USER_ID))
                .isSameAs(forbidden);
            verify(clubApplyRepository, never()).getByIdAndClubId(APPLICATION_ID, CLUB_ID);
        }
    }

    @Nested
    @DisplayName("rejectClubApplication 테스트")
    class RejectTests {

        @Test
        @DisplayName("정상 거절 시 지원서를 삭제한다")
        void rejectClubApplicationWithValidRequestDeletesApply() {
            // Given
            ClubApply apply = ClubApply.builder()
                .id(APPLICATION_ID)
                .club(createClub(CLUB_ID))
                .user(createUser(APPLICANT_ID, "지원자"))
                .build();
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(clubApplyRepository.getByIdAndClubId(APPLICATION_ID, CLUB_ID)).thenReturn(apply);

            // When
            clubApplicationService.rejectClubApplication(CLUB_ID, APPLICATION_ID, USER_ID);

            // Then
            verify(clubApplyRepository).delete(apply);
        }

        @Test
        @DisplayName("권한이 없으면 validator 예외를 전파한다")
        void rejectClubApplicationWithoutLeaderAccessPropagatesException() {
            // Given
            CustomException forbidden = CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            org.mockito.Mockito.doThrow(forbidden)
                .when(clubPermissionValidator)
                .validateLeaderAccess(CLUB_ID, USER_ID);

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.rejectClubApplication(CLUB_ID, APPLICATION_ID, USER_ID))
                .isSameAs(forbidden);
            verify(clubApplyRepository, never()).getByIdAndClubId(APPLICATION_ID, CLUB_ID);
        }
    }

    @Nested
    @DisplayName("getClubApplications 테스트")
    class GetClubApplicationsTests {

        @Test
        @DisplayName("상시모집이면 전체 조회 쿼리를 사용한다")
        void getClubApplicationsWithAlwaysRecruitingUsesFindAllByClubId() {
            // Given
            ClubApplicationCondition condition = new ClubApplicationCondition(
                PAGE,
                LIMIT,
                ClubApplicationSortBy.APPLIED_AT,
                org.springframework.data.domain.Sort.Direction.DESC
            );
            ClubRecruitment recruitment = ClubRecruitment.builder()
                .club(createClub(CLUB_ID))
                .isAlwaysRecruiting(true)
                .content("내용")
                .build();
            Page<ClubApply> emptyPage = new PageImpl<>(List.of());

            when(clubRecruitmentRepository.getByClubId(CLUB_ID)).thenReturn(recruitment);
            when(clubApplyQueryRepository.findAllByClubId(CLUB_ID, condition)).thenReturn(emptyPage);

            // When
            clubApplicationService.getClubApplications(CLUB_ID, USER_ID, condition);

            // Then
            verify(clubApplyQueryRepository).findAllByClubId(CLUB_ID, condition);
            verify(clubApplyQueryRepository, never())
                .findAllByClubIdAndCreatedAtBetween(eq(CLUB_ID), any(LocalDateTime.class), any(LocalDateTime.class),
                    eq(condition));
        }

        @Test
        @DisplayName("기간모집이면 startOfDay와 LocalTime.MAX 경계를 사용한다")
        void getClubApplicationsWithDateRangeUsesBoundaryDateTimes() {
            // Given
            ClubApplicationCondition condition = new ClubApplicationCondition(
                PAGE,
                LIMIT,
                ClubApplicationSortBy.APPLIED_AT,
                org.springframework.data.domain.Sort.Direction.DESC
            );
            LocalDate startDate = LocalDate.of(YEAR, MONTH_JANUARY, START_DAY);
            LocalDate endDate = LocalDate.of(YEAR, MONTH_JANUARY, END_DAY);
            ClubRecruitment recruitment = ClubRecruitment.builder()
                .club(createClub(CLUB_ID))
                .isAlwaysRecruiting(false)
                .startDate(startDate)
                .endDate(endDate)
                .content("내용")
                .build();
            Page<ClubApply> emptyPage = new PageImpl<>(List.of());

            when(clubRecruitmentRepository.getByClubId(CLUB_ID)).thenReturn(recruitment);
            when(clubApplyQueryRepository.findAllByClubIdAndCreatedAtBetween(
                eq(CLUB_ID),
                eq(startDate.atStartOfDay()),
                eq(endDate.atTime(LocalTime.MAX)),
                eq(condition)
            )).thenReturn(emptyPage);

            // When
            clubApplicationService.getClubApplications(CLUB_ID, USER_ID, condition);

            // Then
            verify(clubApplyQueryRepository).findAllByClubIdAndCreatedAtBetween(
                CLUB_ID,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                condition
            );
        }
    }

    @Nested
    @DisplayName("getClubApplicationAnswers 테스트")
    class GetClubApplicationAnswersTests {

        @Test
        @DisplayName("정상 조회 시 질문 순서대로 응답을 구성한다")
        void getClubApplicationAnswersWithValidRequestReturnsAnswers() {
            // Given
            Club club = createClub(CLUB_ID);
            User applicant = createUser(APPLICANT_ID, "지원자");
            ClubApply apply = ClubApply.builder().id(APPLICATION_ID).club(club).user(applicant).build();
            ClubApplyQuestion q1 = ClubApplyQuestion.builder()
                .id(QUESTION_ID)
                .club(club)
                .question("질문1")
                .isRequired(true)
                .build();
            ClubApplyQuestion q2 = ClubApplyQuestion.builder()
                .id(SECOND_QUESTION_ID)
                .club(club)
                .question("질문2")
                .isRequired(false)
                .build();
            ClubApplyAnswer a1 = ClubApplyAnswer.of(apply, q1, "answer1");

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubApplyRepository.getByIdAndClubId(APPLICATION_ID, CLUB_ID)).thenReturn(apply);
            when(clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(CLUB_ID)).thenReturn(List.of(q1, q2));
            when(clubApplyAnswerRepository.findAllByApplyIdWithQuestion(APPLICATION_ID)).thenReturn(List.of(a1));

            // When
            var response = clubApplicationService.getClubApplicationAnswers(CLUB_ID, APPLICATION_ID, USER_ID);

            // Then
            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(response.answers()).hasSize(2);
            assertThat(response.answers().get(0).answer()).isEqualTo("answer1");
            assertThat(response.answers().get(1).answer()).isNull();
        }

        @Test
        @DisplayName("권한이 없으면 validator 예외를 전파한다")
        void getClubApplicationAnswersWithoutManagerAccessPropagatesException() {
            // Given
            CustomException forbidden = CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            org.mockito.Mockito.doThrow(forbidden)
                .when(clubPermissionValidator)
                .validateManagerAccess(CLUB_ID, USER_ID);

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.getClubApplicationAnswers(CLUB_ID, APPLICATION_ID, USER_ID))
                .isSameAs(forbidden);
            verify(clubApplyRepository, never()).getByIdAndClubId(APPLICATION_ID, CLUB_ID);
        }
    }

    @Nested
    @DisplayName("getApplyQuestions 테스트")
    class GetApplyQuestionsTests {

        @Test
        @DisplayName("질문이 존재하면 질문 목록을 반환한다")
        void getApplyQuestionsWithQuestionsReturnsQuestions() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubApplyQuestion q1 = ClubApplyQuestion.builder()
                .id(QUESTION_ID)
                .club(club)
                .question("q1")
                .isRequired(true)
                .build();
            ClubApplyQuestion q2 = ClubApplyQuestion.builder().id(SECOND_QUESTION_ID).club(club).question("q2")
                .isRequired(false).build();
            when(clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(CLUB_ID)).thenReturn(List.of(q1, q2));

            // When
            var response = clubApplicationService.getApplyQuestions(CLUB_ID, USER_ID);

            // Then
            assertThat(response.questions()).hasSize(2);
            assertThat(response.questions().get(0).id()).isEqualTo(QUESTION_ID);
        }
    }

    @Nested
    @DisplayName("replaceApplyQuestions 테스트")
    class ReplaceApplyQuestionsTests {

        @Test
        @DisplayName("중복 questionId가 있으면 DUPLICATE_CLUB_APPLY_QUESTION 예외가 발생한다")
        void replaceApplyQuestionsWithDuplicatedQuestionIdThrowsCustomException() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubApplyQuestion existing = ClubApplyQuestion.builder()
                .id(QUESTION_ID)
                .club(club)
                .question("기존")
                .isRequired(true)
                .build();
            ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
                new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(QUESTION_ID, "수정1", true),
                new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(QUESTION_ID, "수정2", false)
            ));

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(CLUB_ID)).thenReturn(List.of(existing));

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.replaceApplyQuestions(CLUB_ID, USER_ID, request))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(getErrorCode(ex))
                    .isEqualTo(ApiResponseCode.DUPLICATE_CLUB_APPLY_QUESTION));
        }

        @Test
        @DisplayName("존재하지 않는 questionId면 NOT_FOUND_CLUB_APPLY_QUESTION 예외가 발생한다")
        void replaceApplyQuestionsWithMissingQuestionIdThrowsCustomException() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
                new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(QUESTION_ID, "수정", true)
            ));

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(CLUB_ID)).thenReturn(List.of());

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.replaceApplyQuestions(CLUB_ID, USER_ID, request))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(getErrorCode(ex))
                    .isEqualTo(ApiResponseCode.NOT_FOUND_CLUB_APPLY_QUESTION));
        }

        @Test
        @DisplayName("수정/추가/삭제가 혼합된 요청을 정상 처리한다")
        void replaceApplyQuestionsWithMixedRequestsUpdatesCreatesAndDeletesQuestions() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubApplyQuestion existingKeep = ClubApplyQuestion.builder()
                .id(QUESTION_ID)
                .club(club)
                .question("기존1")
                .isRequired(true)
                .build();
            ClubApplyQuestion existingDelete = ClubApplyQuestion.builder()
                .id(SECOND_QUESTION_ID)
                .club(club)
                .question("기존2")
                .isRequired(false)
                .build();
            ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
                new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(QUESTION_ID, "수정질문", false),
                new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(null, "신규질문", true)
            ));

            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(CLUB_ID))
                .thenReturn(List.of(existingKeep, existingDelete))
                .thenReturn(List.of(existingKeep));

            // When
            var response = clubApplicationService.replaceApplyQuestions(CLUB_ID, USER_ID, request);

            // Then
            verify(clubApplyQuestionRepository).saveAll(any());
            verify(clubApplyQuestionRepository).deleteAll(List.of(existingDelete));
            assertThat(response.questions()).hasSize(1);
            assertThat(response.questions().get(0).id()).isEqualTo(QUESTION_ID);
            assertThat(existingKeep.getQuestion()).isEqualTo("수정질문");
            assertThat(existingKeep.getIsRequired()).isFalse();
        }

        @Test
        @DisplayName("신규 질문이 없으면 saveAll을 호출하지 않는다")
        void replaceApplyQuestionsWithoutNewQuestionDoesNotCallSaveAll() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubApplyQuestion existing = ClubApplyQuestion.builder()
                .id(QUESTION_ID)
                .club(club)
                .question("기존")
                .isRequired(true)
                .build();
            ClubApplyQuestionsReplaceRequest request = new ClubApplyQuestionsReplaceRequest(List.of(
                new ClubApplyQuestionsReplaceRequest.ApplyQuestionRequest(QUESTION_ID, "수정", true)
            ));
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(clubApplyQuestionRepository.findAllByClubIdOrderByIdAsc(CLUB_ID))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(existing));

            // When
            clubApplicationService.replaceApplyQuestions(CLUB_ID, USER_ID, request);

            // Then
            verify(clubApplyQuestionRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("getFeeInfo 테스트")
    class GetFeeInfoTests {

        @Test
        @DisplayName("지원자면 회비 정보를 조회할 수 있다")
        void getFeeInfoForAppliedUserReturnsFeeInfo() {
            // Given
            Club club = createClubWithFeeInfo(CLUB_ID);
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "신청자"));
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(true);
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(eq(CLUB_ID), eq(USER_ID), any()))
                .thenReturn(false);

            // When
            var response = clubApplicationService.getFeeInfo(CLUB_ID, USER_ID);

            // Then
            assertThat(response.amount()).isEqualTo(CLUB_FEE_AMOUNT);
        }

        @Test
        @DisplayName("운영진이면 회비 정보를 조회할 수 있다")
        void getFeeInfoForManagerReturnsFeeInfo() {
            // Given
            Club club = createClubWithFeeInfo(CLUB_ID);
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "운영진"));
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(false);
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(eq(CLUB_ID), eq(USER_ID), any()))
                .thenReturn(true);

            // When
            var response = clubApplicationService.getFeeInfo(CLUB_ID, USER_ID);

            // Then
            assertThat(response.bank()).isEqualTo("국민은행");
        }

        @Test
        @DisplayName("지원자도 운영진도 아니면 FORBIDDEN_CLUB_FEE_INFO 예외가 발생한다")
        void getFeeInfoForUnauthorizedUserThrowsCustomException() {
            // Given
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "유저"));
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(false);
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(eq(CLUB_ID), eq(USER_ID), any()))
                .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.getFeeInfo(CLUB_ID, USER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(getErrorCode(ex))
                    .isEqualTo(ApiResponseCode.FORBIDDEN_CLUB_FEE_INFO));
        }
    }

    @Nested
    @DisplayName("replaceFeeInfo 테스트")
    class ReplaceFeeInfoTests {

        @Test
        @DisplayName("정상 요청이면 은행명으로 회비 정보를 갱신한다")
        void replaceFeeInfoWithValidRequestUpdatesClubFeeInfo() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubFeeInfoReplaceRequest request = new ClubFeeInfoReplaceRequest(
                UPDATED_FEE_AMOUNT,
                BANK_ID,
                "123-456",
                "동아리",
                LocalDate.of(YEAR, MONTH_JANUARY, END_DAY)
            );
            Bank bank = mock(Bank.class);
            when(bank.getName()).thenReturn("신한은행");
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "관리자"));
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(bankRepository.getById(BANK_ID)).thenReturn(bank);

            // When
            var response = clubApplicationService.replaceFeeInfo(CLUB_ID, USER_ID, request);

            // Then
            assertThat(response.bank()).isEqualTo("신한은행");
            assertThat(response.amount()).isEqualTo(UPDATED_FEE_AMOUNT);
        }

        @Test
        @DisplayName("권한이 없으면 validator 예외를 전파한다")
        void replaceFeeInfoWithoutManagerAccessPropagatesException() {
            // Given
            CustomException forbidden = CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS);
            ClubFeeInfoReplaceRequest request = new ClubFeeInfoReplaceRequest(
                UPDATED_FEE_AMOUNT,
                BANK_ID,
                "123-456",
                "동아리",
                LocalDate.of(YEAR, MONTH_JANUARY, END_DAY)
            );
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "관리자"));
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            org.mockito.Mockito.doThrow(forbidden)
                .when(clubPermissionValidator)
                .validateManagerAccess(CLUB_ID, USER_ID);

            // When & Then
            assertThatThrownBy(() -> clubApplicationService.replaceFeeInfo(CLUB_ID, USER_ID, request))
                .isSameAs(forbidden);
            verify(bankRepository, never()).getById(BANK_ID);
        }
    }

    private Club createClub(Integer clubId) {
        return Club.builder()
            .id(clubId)
            .name("BCSD")
            .description("설명")
            .introduce("소개")
            .imageUrl("https://img")
            .location("학생회관")
            .clubCategory(ClubCategory.ACADEMIC)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .build();
    }

    private Club createClubWithFeeInfo(Integer clubId) {
        return Club.builder()
            .id(clubId)
            .name("BCSD")
            .description("설명")
            .introduce("소개")
            .imageUrl("https://img")
            .location("학생회관")
            .clubCategory(ClubCategory.ACADEMIC)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .feeAmount(CLUB_FEE_AMOUNT)
            .feeBank("국민은행")
            .feeAccountNumber("100-200")
            .feeAccountHolder("BCSD")
            .feeDeadline(LocalDate.of(YEAR, MONTH_JANUARY, END_DAY))
            .build();
    }

    private User createUser(Integer userId, String name) {
        return User.builder()
            .id(userId)
            .name(name)
            .email(name + "@konect.gg")
            .studentNumber("2020123456")
            .provider(Provider.GOOGLE)
            .providerId("pid-" + userId)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .build();
    }

    private ApiResponseCode getErrorCode(CustomException exception) {
        try {
            Field field = CustomException.class.getDeclaredField("errorCode");
            field.setAccessible(true);
            return ApiResponseCode.class.cast(field.get(exception));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read errorCode", e);
        }
    }
}
