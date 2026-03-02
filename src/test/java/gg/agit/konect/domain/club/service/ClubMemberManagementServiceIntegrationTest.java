package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubPreMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubPreMemberAddResponse;
import gg.agit.konect.domain.club.dto.ClubPreMembersResponse;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

@Transactional
class ClubMemberManagementServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ClubMemberManagementService clubMemberManagementService;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubPreMemberRepository clubPreMemberRepository;

    private University university;
    private Club club;
    private User president;
    private User vicePresident;
    private User manager;
    private User member;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        club = persist(ClubFixture.create(university, "BCSD Lab"));
        president = persist(UserFixture.createUser(university, "회장", "2020000001"));
        vicePresident = persist(UserFixture.createUser(university, "부회장", "2020000002"));
        manager = persist(UserFixture.createUser(university, "매니저", "2020000003"));
        member = persist(UserFixture.createUser(university, "일반멤버", "2021136001"));

        persist(ClubMemberFixture.createPresident(club, president));
        persist(ClubMemberFixture.createVicePresident(club, vicePresident));
        persist(ClubMemberFixture.createManager(club, manager));
        persist(ClubMemberFixture.createMember(club, member));
    }

    @Nested
    @DisplayName("멤버 직책 변경")
    class ChangeMemberPosition {

        @Test
        @DisplayName("회장이 일반 멤버를 매니저로 변경한다")
        void changeMemberToManagerByPresident() {
            // given
            clearPersistenceContext();
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MANAGER);

            // when
            ClubMember result = clubMemberManagementService.changeMemberPosition(
                club.getId(),
                member.getId(),
                president.getId(),
                request
            );

            // then
            assertThat(result.getClubPosition()).isEqualTo(ClubPosition.MANAGER);
        }

        @Test
        @DisplayName("일반 멤버가 직책 변경을 시도하면 예외가 발생한다")
        void changeMemberPositionByMemberFails() {
            // given
            User anotherMember = persist(UserFixture.createUser(university, "다른멤버", "2021136002"));
            persist(ClubMemberFixture.createMember(club, anotherMember));
            clearPersistenceContext();

            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MANAGER);

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.changeMemberPosition(
                club.getId(),
                anotherMember.getId(),
                member.getId(),
                request
            )).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("자기 자신의 직책은 변경할 수 없다")
        void changeSelfPositionFails() {
            // given
            clearPersistenceContext();
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MEMBER);

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.changeMemberPosition(
                club.getId(),
                president.getId(),
                president.getId(),
                request
            )).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("매니저가 자신보다 상위 직책인 부회장을 변경할 수 없다")
        void managerCannotChangeVicePresident() {
            // given
            clearPersistenceContext();
            MemberPositionChangeRequest request = new MemberPositionChangeRequest(ClubPosition.MEMBER);

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.changeMemberPosition(
                club.getId(),
                vicePresident.getId(),
                manager.getId(),
                request
            )).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("회장 위임")
    class TransferPresident {

        @Test
        @DisplayName("회장이 다른 멤버에게 회장을 위임한다")
        void transferPresidentSuccess() {
            // given
            clearPersistenceContext();
            PresidentTransferRequest request = new PresidentTransferRequest(member.getId());

            // when
            List<ClubMember> result = clubMemberManagementService.transferPresident(
                club.getId(),
                president.getId(),
                request
            );

            // then
            assertThat(result).hasSize(2);

            ClubMember formerPresident = clubMemberRepository.getByClubIdAndUserId(club.getId(), president.getId());
            ClubMember newPresident = clubMemberRepository.getByClubIdAndUserId(club.getId(), member.getId());

            assertThat(formerPresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
            assertThat(newPresident.getClubPosition()).isEqualTo(ClubPosition.PRESIDENT);
        }

        @Test
        @DisplayName("회장이 아닌 사람이 회장 위임을 시도하면 예외가 발생한다")
        void transferPresidentByNonPresidentFails() {
            // given
            clearPersistenceContext();
            PresidentTransferRequest request = new PresidentTransferRequest(member.getId());

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.transferPresident(
                club.getId(),
                vicePresident.getId(),
                request
            )).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("자기 자신에게 회장 위임을 시도하면 예외가 발생한다")
        void transferPresidentToSelfFails() {
            // given
            clearPersistenceContext();
            PresidentTransferRequest request = new PresidentTransferRequest(president.getId());

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.transferPresident(
                club.getId(),
                president.getId(),
                request
            )).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("부회장 변경")
    class ChangeVicePresident {

        @Test
        @DisplayName("회장이 부회장을 변경한다")
        void changeVicePresidentSuccess() {
            // given
            clearPersistenceContext();
            VicePresidentChangeRequest request = new VicePresidentChangeRequest(member.getId());

            // when
            List<ClubMember> result = clubMemberManagementService.changeVicePresident(
                club.getId(),
                president.getId(),
                request
            );

            // then
            ClubMember formerVicePresident = clubMemberRepository.getByClubIdAndUserId(
                club.getId(), vicePresident.getId()
            );
            ClubMember newVicePresident = clubMemberRepository.getByClubIdAndUserId(club.getId(), member.getId());

            assertThat(formerVicePresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
            assertThat(newVicePresident.getClubPosition()).isEqualTo(ClubPosition.VICE_PRESIDENT);
        }

        @Test
        @DisplayName("부회장을 해제한다 (null 전달)")
        void removeVicePresident() {
            // given
            clearPersistenceContext();
            VicePresidentChangeRequest request = new VicePresidentChangeRequest(null);

            // when
            List<ClubMember> result = clubMemberManagementService.changeVicePresident(
                club.getId(),
                president.getId(),
                request
            );

            // then
            ClubMember formerVicePresident = clubMemberRepository.getByClubIdAndUserId(
                club.getId(), vicePresident.getId()
            );
            assertThat(formerVicePresident.getClubPosition()).isEqualTo(ClubPosition.MEMBER);
        }
    }

    @Nested
    @DisplayName("멤버 추방")
    class RemoveMember {

        @Test
        @DisplayName("회장이 일반 멤버를 추방한다")
        void removeMemberByPresidentSuccess() {
            // given
            clearPersistenceContext();

            // when
            clubMemberManagementService.removeMember(club.getId(), member.getId(), president.getId());
            clearPersistenceContext();

            // then
            boolean exists = clubMemberRepository.existsByClubIdAndUserId(club.getId(), member.getId());
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("자기 자신을 추방할 수 없다")
        void removeSelfFails() {
            // given
            clearPersistenceContext();

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.removeMember(
                club.getId(), president.getId(), president.getId()
            )).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("회장을 추방할 수 없다")
        void removePresidentFails() {
            // given
            clearPersistenceContext();

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.removeMember(
                club.getId(), president.getId(), vicePresident.getId()
            )).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("매니저 직책의 멤버는 추방할 수 없다")
        void removeManagerFails() {
            // given
            clearPersistenceContext();

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.removeMember(
                club.getId(), manager.getId(), president.getId()
            )).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("사전 멤버 관리")
    class PreMemberManagement {

        @Test
        @DisplayName("사전 멤버를 등록한다")
        void addPreMemberSuccess() {
            // given
            clearPersistenceContext();
            ClubPreMemberAddRequest request = new ClubPreMemberAddRequest(
                "2022000001",
                "신입생",
                ClubPosition.MEMBER
            );

            // when
            ClubPreMemberAddResponse response = clubMemberManagementService.addPreMember(
                club.getId(),
                president.getId(),
                request
            );

            // then
            assertThat(response.name()).isEqualTo("신입생");
            assertThat(response.studentNumber()).isEqualTo("2022000001");
        }

        @Test
        @DisplayName("이미 가입된 사용자를 사전 멤버로 등록하면 바로 멤버로 추가된다")
        void addPreMemberWhenUserExistsBecomesDirectMember() {
            // given
            User existingUser = persist(UserFixture.createUser(university, "기존유저", "2022000002"));
            clearPersistenceContext();

            ClubPreMemberAddRequest request = new ClubPreMemberAddRequest(
                existingUser.getStudentNumber(),
                existingUser.getName(),
                ClubPosition.MEMBER
            );

            // when
            ClubPreMemberAddResponse response = clubMemberManagementService.addPreMember(
                club.getId(),
                president.getId(),
                request
            );

            // then
            assertThat(response.isDirectMember()).isTrue();
            boolean isMember = clubMemberRepository.existsByClubIdAndUserId(club.getId(), existingUser.getId());
            assertThat(isMember).isTrue();
        }

        @Test
        @DisplayName("사전 멤버 목록을 조회한다")
        void getPreMembersSuccess() {
            // given
            ClubPreMemberAddRequest request1 = new ClubPreMemberAddRequest("2022000001", "신입생1", null);
            ClubPreMemberAddRequest request2 = new ClubPreMemberAddRequest("2022000002", "신입생2", null);
            clubMemberManagementService.addPreMember(club.getId(), president.getId(), request1);
            clubMemberManagementService.addPreMember(club.getId(), president.getId(), request2);
            clearPersistenceContext();

            // when
            ClubPreMembersResponse response = clubMemberManagementService.getPreMembers(
                club.getId(), president.getId()
            );

            // then
            assertThat(response.preMembers()).hasSize(2);
        }

        @Test
        @DisplayName("일반 멤버는 사전 멤버를 등록할 수 없다")
        void addPreMemberByMemberFails() {
            // given
            clearPersistenceContext();
            ClubPreMemberAddRequest request = new ClubPreMemberAddRequest("2022000001", "신입생", null);

            // when & then
            assertThatThrownBy(() -> clubMemberManagementService.addPreMember(
                club.getId(), member.getId(), request
            )).isInstanceOf(CustomException.class);
        }
    }
}
