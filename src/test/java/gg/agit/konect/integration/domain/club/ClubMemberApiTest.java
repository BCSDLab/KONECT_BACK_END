package gg.agit.konect.integration.domain.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import gg.agit.konect.domain.club.dto.ClubPreMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubPreMemberBatchAddRequest;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ClubMemberApiTest extends IntegrationTestSupport {

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    private University university;
    private Club club;
    private User president;
    private User vicePresident;
    private User manager;
    private User member;
    private User admin;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        club = persist(ClubFixture.create(university, "BCSD Lab"));
        president = persist(UserFixture.createUser(university, "회장", "2020000001"));
        vicePresident = persist(UserFixture.createUser(university, "부회장", "2020000002"));
        manager = persist(UserFixture.createUser(university, "매니저", "2020000003"));
        member = persist(UserFixture.createUser(university, "일반멤버", "2021136001"));
        admin = persist(UserFixture.createAdmin(university));

        persist(ClubMemberFixture.createPresident(club, president));
        persist(ClubMemberFixture.createVicePresident(club, vicePresident));
        persist(ClubMemberFixture.createManager(club, manager));
        persist(ClubMemberFixture.createMember(club, member));
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/members/{userId}/position - 멤버 직책 변경")
    class ChangeMemberPosition {

        @Test
        @DisplayName("회장이 일반 멤버를 매니저로 변경한다")
        void changeMemberToManagerByPresident() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MANAGER);

            // when & then
            performPatch("/clubs/" + club.getId() + "/members/" + member.getId() + "/position", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("MANAGER"));
        }

        @Test
        @DisplayName("관리자는 동아리 회원이 아니어도 멤버 직책을 변경할 수 있다")
        void adminCanChangeMemberPositionWithoutClubMembership() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(admin.getId());

            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MANAGER);

            // when & then
            performPatch("/clubs/" + club.getId() + "/members/" + member.getId() + "/position", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("MANAGER"));
        }

        @Test
        @DisplayName("일반 멤버가 직책 변경을 시도하면 403을 반환한다")
        void changeMemberPositionByMemberFails() throws Exception {
            // given
            User anotherMember = persist(UserFixture.createUser(university, "다른멤버", "2021136002"));
            persist(ClubMemberFixture.createMember(club, anotherMember));
            clearPersistenceContext();

            mockLoginUser(member.getId());

            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MANAGER);

            // when & then
            performPatch("/clubs/" + club.getId() + "/members/" + anotherMember.getId() + "/position", request)
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("자기 자신의 직책은 변경할 수 없다")
        void changeSelfPositionFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MEMBER);

            // when & then
            performPatch("/clubs/" + club.getId() + "/members/" + president.getId() + "/position", request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("매니저가 자신보다 상위 직책인 부회장을 변경하려 하면 403을 반환한다")
        void managerCannotChangeVicePresident() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(manager.getId());

            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MEMBER);

            // when & then
            // 매니저는 직책 변경 권한이 없으므로 권한 체크에서 403이 먼저 반환됨
            performPatch("/clubs/" + club.getId() + "/members/" + vicePresident.getId() + "/position", request)
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /clubs/{clubId}/president/transfer - 회장 위임")
    class TransferPresident {

        @Test
        @DisplayName("회장이 다른 멤버에게 회장을 위임한다")
        void transferPresidentSuccess() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            PresidentTransferRequest request = new PresidentTransferRequest(member.getId());

            // when & then
            performPost("/clubs/" + club.getId() + "/president/transfer", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changedMembers", hasSize(2)));

            clearPersistenceContext();

            ClubMember formerPresident = clubMemberRepository.getByClubIdAndUserId(club.getId(), president.getId());
            ClubMember newPresident = clubMemberRepository.getByClubIdAndUserId(club.getId(), member.getId());

            assertThat(formerPresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
            assertThat(newPresident.getClubPosition()).isEqualTo(ClubPosition.PRESIDENT);
        }

        @Test
        @DisplayName("회장이 아닌 사람이 회장 위임을 시도하면 403을 반환한다")
        void transferPresidentByNonPresidentFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(vicePresident.getId());

            PresidentTransferRequest request = new PresidentTransferRequest(member.getId());

            // when & then
            performPost("/clubs/" + club.getId() + "/president/transfer", request)
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("자기 자신에게 회장 위임을 시도하면 400을 반환한다")
        void transferPresidentToSelfFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            PresidentTransferRequest request = new PresidentTransferRequest(president.getId());

            // when & then
            performPost("/clubs/" + club.getId() + "/president/transfer", request)
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/vice-president - 부회장 변경")
    class ChangeVicePresident {

        @Test
        @DisplayName("회장이 부회장을 변경한다")
        void changeVicePresidentSuccess() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            VicePresidentChangeRequest request = new VicePresidentChangeRequest(member.getId());

            // when & then
            performPatch("/clubs/" + club.getId() + "/vice-president", request)
                .andExpect(status().isOk());

            clearPersistenceContext();

            ClubMember formerVicePresident = clubMemberRepository.getByClubIdAndUserId(
                club.getId(), vicePresident.getId()
            );
            ClubMember newVicePresident = clubMemberRepository.getByClubIdAndUserId(club.getId(), member.getId());

            assertThat(formerVicePresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
            assertThat(newVicePresident.getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
        }

        @Test
        @DisplayName("부회장을 해제한다")
        void removeVicePresident() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            VicePresidentChangeRequest request = new VicePresidentChangeRequest(null);

            // when & then
            performPatch("/clubs/" + club.getId() + "/vice-president", request)
                .andExpect(status().isOk());

            clearPersistenceContext();

            ClubMember formerVicePresident = clubMemberRepository.getByClubIdAndUserId(
                club.getId(), vicePresident.getId()
            );
            assertThat(formerVicePresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
        }
    }

    @Nested
    @DisplayName("DELETE /clubs/{clubId}/members/{userId} - 멤버 추방")
    class RemoveMember {

        @Test
        @DisplayName("회장이 일반 멤버를 추방한다")
        void removeMemberByPresidentSuccess() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            // when & then
            performDelete("/clubs/" + club.getId() + "/members/" + member.getId())
                .andExpect(status().isNoContent());

            clearPersistenceContext();

            boolean exists = clubMemberRepository.existsByClubIdAndUserId(club.getId(), member.getId());
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("자기 자신을 추방할 수 없다")
        void removeSelfFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            // when & then
            performDelete("/clubs/" + club.getId() + "/members/" + president.getId())
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("회장을 추방할 수 없다")
        void removePresidentFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(vicePresident.getId());

            // when & then
            performDelete("/clubs/" + club.getId() + "/members/" + president.getId())
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("매니저 직책의 멤버는 추방할 수 없다")
        void removeManagerFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            // when & then
            performDelete("/clubs/" + club.getId() + "/members/" + manager.getId())
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /clubs/{clubId}/pre-members - 사전 멤버 등록")
    class PreMemberManagement {

        @Test
        @DisplayName("사전 멤버를 등록한다")
        void addPreMemberSuccess() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            ClubPreMemberAddRequest request = new ClubPreMemberAddRequest(
                "2022000001",
                "신입생",
                ClubPosition.MEMBER
            );

            // when & then
            performPost("/clubs/" + club.getId() + "/pre-members", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("신입생"))
                .andExpect(jsonPath("$.studentNumber").value("2022000001"));
        }

        @Test
        @DisplayName("이미 가입된 사용자를 사전 멤버로 등록하면 바로 멤버로 추가된다")
        void addPreMemberWhenUserExistsBecomesDirectMember() throws Exception {
            // given
            User existingUser = persist(UserFixture.createUser(university, "기존유저", "2022000002"));
            clearPersistenceContext();

            mockLoginUser(president.getId());

            ClubPreMemberAddRequest request = new ClubPreMemberAddRequest(
                existingUser.getStudentNumber(),
                existingUser.getName(),
                ClubPosition.MEMBER
            );

            // when & then
            performPost("/clubs/" + club.getId() + "/pre-members", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDirectMember").value(true));

            boolean isMember = clubMemberRepository.existsByClubIdAndUserId(club.getId(), existingUser.getId());
            assertThat(isMember).isTrue();
        }

        @Test
        @DisplayName("일반 멤버는 사전 멤버를 등록할 수 없다")
        void addPreMemberByMemberFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(member.getId());

            ClubPreMemberAddRequest request = new ClubPreMemberAddRequest("2022000001", "신입생", null);

            // when & then
            performPost("/clubs/" + club.getId() + "/pre-members", request)
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /clubs/{clubId}/pre-members/batch - 사전 멤버 일괄 등록")
    class AddPreMembersBatch {

        @Test
        @DisplayName("여러 명의 사전 멤버를 일괄 등록한다")
        void addPreMembersBatchSuccess() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            String requestBody = """
                {"members": [
                    {"studentNumber": "2022000001", "name": "신입생1", "clubPosition": "MEMBER"},
                    {"studentNumber": "2022000002", "name": "신입생2", "clubPosition": "MEMBER"},
                    {"studentNumber": "2022000003", "name": "신입생3", "clubPosition": "MANAGER"}
                ]}
                """;

            // when & then
            mockMvc.perform(post("/clubs/" + club.getId() + "/pre-members/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.successCount").value(3))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[0].success").value(true))
                .andExpect(jsonPath("$.results[1].success").value(true))
                .andExpect(jsonPath("$.results[2].success").value(true));
        }

        @Test
        @DisplayName("일부 실패 시에도 나머지는 등록된다 (부분 성공)")
        void addPreMembersBatchPartialSuccess() throws Exception {
            // given - 이미 존재하는 사용자를 먼저 사전 등록
            User existingUser = persist(UserFixture.createUser(university, "기존유저", "2022000002"));
            clearPersistenceContext();
            mockLoginUser(president.getId());

            // 먼저 첫 번째 멤버를 등록하여 중복 상황 만들기
            String firstRequest = """
                {"members": [{"studentNumber": "%s", "name": "%s", "clubPosition": "MEMBER"}]}
                """.formatted(existingUser.getStudentNumber(), existingUser.getName());
            mockMvc.perform(post("/clubs/" + club.getId() + "/pre-members/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstRequest));
            clearPersistenceContext();
            mockLoginUser(president.getId());

            // 같은 멤버를 다시 배치 등록 시도 (첫 번째는 이미 존재해서 실패, 두 번째는 성공)
            String batchRequest = """
                {"members": [
                    {"studentNumber": "%s", "name": "%s", "clubPosition": "MEMBER"},
                    {"studentNumber": "2022000003", "name": "신입생", "clubPosition": "MEMBER"}
                ]}
                """.formatted(existingUser.getStudentNumber(), existingUser.getName());

            // when & then
            mockMvc.perform(post("/clubs/" + club.getId() + "/pre-members/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(batchRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.results[0].success").value(false))
                .andExpect(jsonPath("$.results[1].success").value(true));
        }

        @Test
        @DisplayName("50명 초과 요청 시 400을 반환한다")
        void addPreMembersBatchExceedsLimit() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            List<ClubPreMemberAddRequest> members = new ArrayList<>();
            for (int i = 0; i < 51; i++) {
                members.add(
                    new ClubPreMemberAddRequest("2022" + String.format("%06d", i), "학생" + i, ClubPosition.MEMBER));
            }
            ClubPreMemberBatchAddRequest request = new ClubPreMemberBatchAddRequest(members);

            // when & then
            performPost("/clubs/" + club.getId() + "/pre-members/batch", request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("일반 멤버는 배치 등록을 할 수 없다")
        void addPreMembersBatchByMemberFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(member.getId());

            List<ClubPreMemberAddRequest> members = List.of(
                new ClubPreMemberAddRequest("2022000001", "신입생", ClubPosition.MEMBER)
            );
            ClubPreMemberBatchAddRequest request = new ClubPreMemberBatchAddRequest(members);

            // when & then
            performPost("/clubs/" + club.getId() + "/pre-members/batch", request)
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("빈 리스트 요청 시 400을 반환한다")
        void addPreMembersBatchEmptyListFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            ClubPreMemberBatchAddRequest request = new ClubPreMemberBatchAddRequest(List.of());

            // when & then
            performPost("/clubs/" + club.getId() + "/pre-members/batch", request)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("null 리스트 요청 시 400을 반환한다")
        void addPreMembersBatchNullListFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            // JSON에서 members를 null로 설정하여 요청
            String requestBody = """
                {"members": null}
                """;

            // when & then
            mockMvc.perform(post("/clubs/" + club.getId() + "/pre-members/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("null 요소가 포함된 리스트는 400을 반환한다")
        void addPreMembersBatchNullElementFails() throws Exception {
            // given
            clearPersistenceContext();
            mockLoginUser(president.getId());

            // 리스트에 null 요소가 포함된 요청
            String requestBody = """
                {"members": [null]}
                """;

            // when & then
            mockMvc.perform(post("/clubs/" + club.getId() + "/pre-members/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/pre-members - 사전 멤버 조회")
    class GetPreMembers {

        @Test
        @DisplayName("사전 멤버 목록을 조회한다")
        void getPreMembersSuccess() throws Exception {
            // given
            mockLoginUser(president.getId());

            // when & then - 사전 멤버가 없는 경우 빈 리스트 반환
            performGet("/clubs/" + club.getId() + "/pre-members")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preMembers").isArray())
                .andExpect(jsonPath("$.preMembers", hasSize(0)));
        }
    }
}
