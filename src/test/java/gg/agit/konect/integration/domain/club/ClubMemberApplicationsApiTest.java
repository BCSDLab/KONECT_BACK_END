package gg.agit.konect.integration.domain.club;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.repository.ClubApplyAnswerRepository;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.ClubRecruitmentFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@DisplayName("동아리 승인 멤버 지원서 API 테스트")
class ClubMemberApplicationsApiTest extends IntegrationTestSupport {

    @Autowired
    private ClubApplyRepository clubApplyRepository;

    @Autowired
    private ClubApplyAnswerRepository clubApplyAnswerRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    private University university;
    private Club club;
    private User president;
    private ClubRecruitment recruitment;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        president = persist(UserFixture.createUser(university, "회장", "2020000001"));
        club = persist(ClubFixture.createWithRecruitment(university, "BCSD Lab"));
        persist(ClubMemberFixture.createPresident(club, president));
        recruitment = persist(ClubRecruitmentFixture.createAlwaysRecruiting(club));
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/member-applications - 승인된 멤버 지원서 목록")
    class GetApprovedMemberApplications {

        @Test
        @DisplayName("승인된 멤버들의 지원서 목록을 조회한다")
        void getApprovedMemberApplicationsSuccess() throws Exception {
            // given
            User approvedUser1 = persist(UserFixture.createUser(university, "승인자1", "2021000001"));
            User approvedUser2 = persist(UserFixture.createUser(university, "승인자2", "2021000002"));
            User pendingUser = persist(UserFixture.createUser(university, "대기자", "2021000003"));

            // 승인된 지원서
            ClubApply approvedApply1 = ClubApply.of(club, approvedUser1, null);
            ClubApply approvedApply2 = ClubApply.of(club, approvedUser2, null);
            approvedApply1.approve();
            approvedApply2.approve();
            persist(approvedApply1);
            persist(approvedApply2);

            // 멤버로 등록
            persist(ClubMemberFixture.createMember(club, approvedUser1));
            persist(ClubMemberFixture.createMember(club, approvedUser2));

            // 대기중인 지원서
            persist(ClubApply.of(club, pendingUser, null));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", hasSize(2)))
                .andExpect(jsonPath("$.applications[0].name").exists())
                .andExpect(jsonPath("$.applications[1].name").exists());
        }

        @Test
        @DisplayName("승인된 멤버가 없으면 빈 목록을 반환한다")
        void getApprovedMemberApplicationsWhenEmpty() throws Exception {
            // given
            User pendingUser = persist(UserFixture.createUser(university, "대기자", "2021000004"));
            persist(ClubApply.of(club, pendingUser, null));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", hasSize(0)));
        }

        @Test
        @DisplayName("페이지네이션이 정상 동작한다")
        void paginationWorks() throws Exception {
            // given - 15명의 승인된 멤버 생성
            for (int i = 1; i <= 15; i++) {
                User user = persist(UserFixture.createUser(university, "승인자" + i, "202100" + String.format("%04d", i)));
                ClubApply apply = ClubApply.of(club, user, null);
                apply.approve();
                persist(apply);
                persist(ClubMemberFixture.createMember(club, user));
            }
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then - 첫 페이지 (10개)
            performGet("/clubs/" + club.getId() + "/member-applications?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", hasSize(10)))
                .andExpect(jsonPath("$.totalCount").value(15));

            // 두 페이지 (5개)
            performGet("/clubs/" + club.getId() + "/member-applications?page=2&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", hasSize(5)));
        }

        @Test
        @DisplayName("권한 없는 사용자는 조회할 수 없다")
        void getApprovedMemberApplicationsWithoutPermissionFails() throws Exception {
            // given
            User regularMember = persist(UserFixture.createUser(university, "일반회원", "2021000005"));
            persist(ClubMemberFixture.createMember(club, regularMember));
            clearPersistenceContext();

            mockLoginUser(regularMember.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications?page=1&limit=10")
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("동아리에 속하지 않은 사용자는 조회할 수 없다")
        void getApprovedMemberApplicationsByNonMemberFails() throws Exception {
            // given
            User outsider = persist(UserFixture.createUser(university, "외부인", "2021000006"));
            clearPersistenceContext();

            mockLoginUser(outsider.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications?page=1&limit=10")
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/member-applications/{userId}/answers - 특정 멤버 지원서 답변 조회")
    class GetApprovedMemberApplicationAnswers {

        @Test
        @DisplayName("승인된 멤버의 지원서 답변을 조회한다")
        void getApprovedMemberApplicationAnswersSuccess() throws Exception {
            // given
            User approvedUser = persist(UserFixture.createUser(university, "승인자", "2021000007"));

            ClubApplyQuestion question1 = persist(ClubApplyQuestion.of(club, "지원 동기", true, 1));
            ClubApplyQuestion question2 = persist(ClubApplyQuestion.of(club, "관심 분야", false, 2));

            ClubApply approvedApply = ClubApply.of(club, approvedUser, null);
            approvedApply.approve();
            persist(approvedApply);
            persist(ClubMemberFixture.createMember(club, approvedUser));

            ClubApplyAnswer answer1 = ClubApplyAnswer.of(approvedApply, question1, "동아리 활동에 관심이 있습니다.");
            ClubApplyAnswer answer2 = ClubApplyAnswer.of(approvedApply, question2, "백엔드 개발");
            persist(answer1);
            persist(answer2);
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications/" + approvedUser.getId() + "/answers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(approvedUser.getId()))
                .andExpect(jsonPath("$.name").value("승인자"))
                .andExpect(jsonPath("$.answers", hasSize(2)))
                .andExpect(jsonPath("$.answers[0].question").exists())
                .andExpect(jsonPath("$.answers[0].answer").exists());
        }

        @Test
        @DisplayName("질문이 없는 지원서도 조회할 수 있다")
        void getApprovedMemberApplicationAnswersWithoutQuestions() throws Exception {
            // given
            User approvedUser = persist(UserFixture.createUser(university, "질문없음", "2021000008"));

            ClubApply approvedApply = ClubApply.of(club, approvedUser, null);
            approvedApply.approve();
            persist(approvedApply);
            persist(ClubMemberFixture.createMember(club, approvedUser));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications/" + approvedUser.getId() + "/answers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers", hasSize(0)));
        }

        @Test
        @DisplayName("대기중인 지원자의 답변은 조회할 수 없다")
        void getPendingMemberApplicationAnswersFails() throws Exception {
            // given
            User pendingUser = persist(UserFixture.createUser(university, "대기자", "2021000009"));
            persist(ClubApply.of(club, pendingUser, null));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications/" + pendingUser.getId() + "/answers")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("동아리 멤버가 아닌 사용자의 답변은 조회할 수 없다")
        void getNonMemberApplicationAnswersFails() throws Exception {
            // given
            User nonMember = persist(UserFixture.createUser(university, "비회원", "2021000010"));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications/" + nonMember.getId() + "/answers")
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("시간이 지난 후 수정된 질문은 지원서에 반영되지 않는다")
        void questionsModifiedAfterApplicationAreNotShown() throws Exception {
            // given
            User approvedUser = persist(UserFixture.createUser(university, "과거신청", "2021000011"));

            // 과거에 등록된 질문
            ClubApplyQuestion oldQuestion = ClubApplyQuestion.of(club, "구버전 질문", true, 1);
            persist(oldQuestion);

            ClubApply approvedApply = ClubApply.of(club, approvedUser, null);
            approvedApply.approve();
            persist(approvedApply);
            persist(ClubMemberFixture.createMember(club, approvedUser));

            ClubApplyAnswer answer = ClubApplyAnswer.of(approvedApply, oldQuestion, "과거 답변");
            persist(answer);

            // 새로운 질문으로 교체
            oldQuestion.softDelete(LocalDateTime.now());
            persist(oldQuestion);
            persist(ClubApplyQuestion.of(club, "신규 질문", true, 1));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications/" + approvedUser.getId() + "/answers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers", hasSize(1)))
                .andExpect(jsonPath("$.answers[0].question").value("구버전 질문"));
        }
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/member-applications/answers - 승인 멤버 지원서 목록 일괄 조회")
    class GetApprovedMemberApplicationAnswersList {

        @Test
        @DisplayName("승인된 멤버들의 지원서 목록을 일괄 조회한다")
        void getApprovedMemberApplicationAnswersListSuccess() throws Exception {
            // given
            User approvedUser1 = persist(UserFixture.createUser(university, "일괄승인1", "2021000012"));
            User approvedUser2 = persist(UserFixture.createUser(university, "일괄승인2", "2021000013"));

            ClubApplyQuestion question = persist(ClubApplyQuestion.of(club, "공통질문", true, 1));

            ClubApply apply1 = ClubApply.of(club, approvedUser1, null);
            ClubApply apply2 = ClubApply.of(club, approvedUser2, null);
            apply1.approve();
            apply2.approve();
            persist(apply1);
            persist(apply2);

            persist(ClubMemberFixture.createMember(club, approvedUser1));
            persist(ClubMemberFixture.createMember(club, approvedUser2));

            persist(ClubApplyAnswer.of(apply1, question, "답변1"));
            persist(ClubApplyAnswer.of(apply2, question, "답변2"));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            // when & then
            performGet("/clubs/" + club.getId() + "/member-applications/answers?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", hasSize(2)))
                .andExpect(jsonPath("$.applications[0].answers", hasSize(1)))
                .andExpect(jsonPath("$.applications[1].answers", hasSize(1)));
        }
    }
}
