package gg.agit.konect.integration.domain.club;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.club.dto.ClubSettingsUpdateRequest;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.ClubRecruitmentFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

@DisplayName("ClubSettingsController 통합 테스트")
class ClubSettingsControllerTest extends IntegrationTestSupport {

    private University university;
    private User president;
    private User vicePresident;
    private User manager;
    private User regularMember;
    private User nonMember;
    private Club club;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());

        president = persist(UserFixture.createUser(university, "회장", "2020000001"));
        vicePresident = persist(UserFixture.createUser(university, "부회장", "2020000002"));
        manager = persist(UserFixture.createUser(university, "운영진", "2020000003"));
        regularMember = persist(UserFixture.createUser(university, "일반회원", "2020000004"));
        nonMember = persist(UserFixture.createUser(university, "비회원", "2020000005"));

        club = persist(ClubFixture.createWithRecruitment(university, "테스트 동아리"));

        persist(ClubMemberFixture.createPresident(club, president));
        persist(ClubMemberFixture.createVicePresident(club, vicePresident));
        persist(ClubMemberFixture.createManager(club, manager));
        persist(ClubMemberFixture.createMember(club, regularMember));

        clearPersistenceContext();
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/settings - 동아리 설정 조회")
    class GetSettings {

        @Test
        @DisplayName("회장 권한으로 설정 조회에 성공한다")
        void getSettingsAsPresident() throws Exception {
            mockLoginUser(president.getId());

            performGet("/clubs/" + club.getId() + "/settings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(true))
                .andExpect(jsonPath("$.isApplicationEnabled").value(true))
                .andExpect(jsonPath("$.isFeeEnabled").value(false))
                .andExpect(jsonPath("$.application").exists())
                .andExpect(jsonPath("$.application.questionCount").isNumber())
                .andExpect(jsonPath("$.fee").doesNotExist());
        }

        @Test
        @DisplayName("부회장 권한으로 설정 조회에 성공한다")
        void getSettingsAsVicePresident() throws Exception {
            mockLoginUser(vicePresident.getId());

            performGet("/clubs/" + club.getId() + "/settings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(true));
        }

        @Test
        @DisplayName("운영진 권한으로 설정 조회에 성공한다")
        void getSettingsAsManager() throws Exception {
            mockLoginUser(manager.getId());

            performGet("/clubs/" + club.getId() + "/settings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(true));
        }

        @Test
        @DisplayName("일반 회원은 설정 조회에 실패한다 (403 Forbidden)")
        void getSettingsAsRegularMemberFails() throws Exception {
            mockLoginUser(regularMember.getId());

            performGet("/clubs/" + club.getId() + "/settings")
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("비회원은 설정 조회에 실패한다 (403 Forbidden)")
        void getSettingsAsNonMemberFails() throws Exception {
            mockLoginUser(nonMember.getId());

            performGet("/clubs/" + club.getId() + "/settings")
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 동아리 조회 시 404를 반환한다")
        void getSettingsNotFoundClub() throws Exception {
            mockLoginUser(president.getId());

            performGet("/clubs/99999/settings")
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("모집공고가 설정된 동아리는 recruitment 필드를 반환한다")
        void getSettingsWithRecruitmentInfo() throws Exception {
            persist(ClubRecruitmentFixture.createAlwaysRecruiting(club));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            performGet("/clubs/" + club.getId() + "/settings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(true))
                .andExpect(jsonPath("$.recruitment").exists())
                .andExpect(jsonPath("$.recruitment.isAlwaysRecruiting").value(true))
                .andExpect(jsonPath("$.recruitment.startAt").doesNotExist())
                .andExpect(jsonPath("$.recruitment.endAt").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/settings - 동아리 설정 수정")
    class UpdateSettings {

        @Test
        @DisplayName("회장 권한으로 설정 수정에 성공한다")
        void updateSettingsAsPresident() throws Exception {
            mockLoginUser(president.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                false,
                false,
                true
            );

            performPatch("/clubs/" + club.getId() + "/settings", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(false))
                .andExpect(jsonPath("$.isApplicationEnabled").value(false))
                .andExpect(jsonPath("$.isFeeEnabled").value(true));
        }

        @Test
        @DisplayName("부회장 권한으로 설정 수정에 성공한다")
        void updateSettingsAsVicePresident() throws Exception {
            mockLoginUser(vicePresident.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                true,
                false,
                false
            );

            performPatch("/clubs/" + club.getId() + "/settings", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isApplicationEnabled").value(false));
        }

        @Test
        @DisplayName("운영진 권한으로 설정 수정에 성공한다")
        void updateSettingsAsManager() throws Exception {
            mockLoginUser(manager.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                false,
                true,
                false
            );

            performPatch("/clubs/" + club.getId() + "/settings", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(false));
        }

        @Test
        @DisplayName("일부 필드만 수정하면 해당 필드만 변경된다")
        void updatePartialSettings() throws Exception {
            mockLoginUser(president.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                false,
                null,
                null
            );

            performPatch("/clubs/" + club.getId() + "/settings", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(false))
                .andExpect(jsonPath("$.isApplicationEnabled").value(true))
                .andExpect(jsonPath("$.isFeeEnabled").value(false));
        }

        @Test
        @DisplayName("일반 회원은 설정 수정에 실패한다 (403 Forbidden)")
        void updateSettingsAsRegularMemberFails() throws Exception {
            mockLoginUser(regularMember.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                false,
                false,
                false
            );

            performPatch("/clubs/" + club.getId() + "/settings", request)
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("비회원은 설정 수정에 실패한다 (403 Forbidden)")
        void updateSettingsAsNonMemberFails() throws Exception {
            mockLoginUser(nonMember.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                false,
                false,
                false
            );

            performPatch("/clubs/" + club.getId() + "/settings", request)
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 동아리 수정 시 404를 반환한다")
        void updateSettingsNotFoundClub() throws Exception {
            mockLoginUser(president.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                false,
                false,
                false
            );

            performPatch("/clubs/99999/settings", request)
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("동일한 설정값으로 수정해도 성공한다")
        void updateSettingsWithSameValues() throws Exception {
            mockLoginUser(president.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                true,
                true,
                false
            );

            performPatch("/clubs/" + club.getId() + "/settings", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(true))
                .andExpect(jsonPath("$.isApplicationEnabled").value(true))
                .andExpect(jsonPath("$.isFeeEnabled").value(false));
        }
    }

    @Nested
    @DisplayName("권한 경계 테스트")
    class PermissionBoundaryTests {

        @Test
        @DisplayName("모든 토글을 동시에 변경할 수 있다")
        void updateAllTogglesAtOnce() throws Exception {
            mockLoginUser(president.getId());

            ClubSettingsUpdateRequest request = new ClubSettingsUpdateRequest(
                false,
                false,
                true
            );

            performPatch("/clubs/" + club.getId() + "/settings", request)
                .andExpect(status().isOk());

            clearPersistenceContext();
            mockLoginUser(president.getId());

            performGet("/clubs/" + club.getId() + "/settings")
                .andExpect(jsonPath("$.isRecruitmentEnabled").value(false))
                .andExpect(jsonPath("$.isApplicationEnabled").value(false))
                .andExpect(jsonPath("$.isFeeEnabled").value(true));
        }
    }
}
