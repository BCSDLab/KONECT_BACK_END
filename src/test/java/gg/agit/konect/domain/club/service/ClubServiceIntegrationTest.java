package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubCondition;
import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.dto.ClubDetailResponse;
import gg.agit.konect.domain.club.dto.ClubsResponse;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

@Transactional
class ClubServiceIntegrationTest extends IntegrationTestSupport {

    private static final int DEFAULT_PAGE_SIZE = 10;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    private University university;
    private User normalUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136001"));
        adminUser = persist(UserFixture.createAdmin(university));
    }

    @Nested
    @DisplayName("동아리 목록 조회")
    class GetClubs {

        @Test
        @DisplayName("동아리 목록을 조회한다")
        void getClubsSuccess() {
            // given
            Club club1 = persist(ClubFixture.create(university, "BCSD Lab"));
            Club club2 = persist(ClubFixture.create(university, "스타트업 동아리"));
            User president1 = persist(UserFixture.createUser(university, "회장1", "2020000001"));
            User president2 = persist(UserFixture.createUser(university, "회장2", "2020000002"));
            persist(ClubMemberFixture.createPresident(club1, president1));
            persist(ClubMemberFixture.createPresident(club2, president2));
            clearPersistenceContext();

            ClubCondition condition = new ClubCondition(1, DEFAULT_PAGE_SIZE, null, null);

            // when
            ClubsResponse response = clubService.getClubs(condition, normalUser.getId());

            // then
            assertThat(response.clubs()).hasSize(2);
            assertThat(response.totalPage()).isEqualTo(1);
        }

        @Test
        @DisplayName("검색어로 동아리를 필터링한다")
        void getClubsWithQuery() {
            // given
            Club club1 = persist(ClubFixture.create(university, "BCSD Lab"));
            Club club2 = persist(ClubFixture.create(university, "스타트업 동아리"));
            User president1 = persist(UserFixture.createUser(university, "회장1", "2020000001"));
            User president2 = persist(UserFixture.createUser(university, "회장2", "2020000002"));
            persist(ClubMemberFixture.createPresident(club1, president1));
            persist(ClubMemberFixture.createPresident(club2, president2));
            clearPersistenceContext();

            ClubCondition condition = new ClubCondition(1, DEFAULT_PAGE_SIZE, "BCSD", null);

            // when
            ClubsResponse response = clubService.getClubs(condition, normalUser.getId());

            // then
            assertThat(response.clubs()).hasSize(1);
            assertThat(response.clubs().get(0).name()).isEqualTo("BCSD Lab");
        }
    }

    @Nested
    @DisplayName("동아리 상세 조회")
    class GetClubDetail {

        @Test
        @DisplayName("동아리 상세 정보를 조회한다")
        void getClubDetailSuccess() {
            // given
            Club club = persist(ClubFixture.create(university, "BCSD Lab"));
            User president = persist(UserFixture.createUser(university, "회장", "2020000001"));
            persist(ClubMemberFixture.createPresident(club, president));
            clearPersistenceContext();

            // when
            ClubDetailResponse response = clubService.getClubDetail(club.getId(), normalUser.getId());

            // then
            assertThat(response.name()).isEqualTo("BCSD Lab");
            assertThat(response.presidentName()).isEqualTo("회장");
            assertThat(response.memberCount()).isEqualTo(1);
            assertThat(response.isMember()).isFalse();
        }

        @Test
        @DisplayName("동아리 멤버인 경우 isMember가 true이다")
        void getClubDetailAsMember() {
            // given
            Club club = persist(ClubFixture.create(university, "BCSD Lab"));
            persist(ClubMemberFixture.createPresident(club, normalUser));
            clearPersistenceContext();

            // when
            ClubDetailResponse response = clubService.getClubDetail(club.getId(), normalUser.getId());

            // then
            assertThat(response.isMember()).isTrue();
        }
    }

    @Nested
    @DisplayName("동아리 생성 (어드민)")
    class CreateClub {

        @Test
        @DisplayName("어드민이 동아리를 생성한다")
        void createClubByAdminSuccess() {
            // given
            User presidentUser = persist(UserFixture.createUser(university, "새회장", "2020000003"));
            clearPersistenceContext();

            ClubCreateRequest request = new ClubCreateRequest(
                presidentUser.getId(),
                "새 동아리",
                "동아리 소개",
                "상세 소개",
                "https://example.com/image.png",
                "학생회관 201호",
                ClubCategory.ACADEMIC
            );

            // when
            ClubDetailResponse response = clubService.createClub(adminUser.getId(), request);

            // then
            assertThat(response.name()).isEqualTo("새 동아리");
            assertThat(response.presidentName()).isEqualTo("새회장");
            assertThat(response.memberCount()).isEqualTo(1);

            // 동아리가 저장되었는지 확인
            Club savedClub = clubRepository.getById(response.id());
            assertThat(savedClub).isNotNull();

            // 회장이 멤버로 등록되었는지 확인
            ClubMember president = clubMemberRepository.getByClubIdAndUserId(
                response.id(),
                presidentUser.getId()
            );
            assertThat(president.isPresident()).isTrue();
        }

        @Test
        @DisplayName("일반 유저가 동아리 생성 시 예외가 발생한다")
        void createClubByNormalUserFails() {
            // given
            ClubCreateRequest request = new ClubCreateRequest(
                normalUser.getId(),
                "새 동아리",
                "동아리 소개",
                "상세 소개",
                "https://example.com/image.png",
                "학생회관 201호",
                ClubCategory.ACADEMIC
            );

            // when & then
            assertThatThrownBy(() -> clubService.createClub(normalUser.getId(), request))
                .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("가입한 동아리 조회")
    class GetJoinedClubs {

        @Test
        @DisplayName("사용자가 가입한 동아리 목록을 조회한다")
        void getJoinedClubsSuccess() {
            // given
            Club club1 = persist(ClubFixture.create(university, "동아리1"));
            Club club2 = persist(ClubFixture.create(university, "동아리2"));
            persist(ClubMemberFixture.createMember(club1, normalUser));
            persist(ClubMemberFixture.createMember(club2, normalUser));
            clearPersistenceContext();

            // when
            var response = clubService.getJoinedClubs(normalUser.getId());

            // then
            assertThat(response.joinedClubs()).hasSize(2);
        }
    }
}
