package gg.agit.konect.domain.studytime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.studytime.dto.StudyTimeMyRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeMyRankingsResponse;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingCondition;
import gg.agit.konect.domain.studytime.dto.StudyTimeRankingsResponse;
import gg.agit.konect.domain.studytime.enums.StudyTimeRankingSort;
import gg.agit.konect.domain.studytime.model.RankingType;
import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.studytime.repository.RankingTypeRepository;
import gg.agit.konect.domain.studytime.repository.StudyTimeRankingRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class StudyTimeRankingServiceTest extends ServiceTestSupport {

    @Mock
    private StudyTimeRankingRepository studyTimeRankingRepository;

    @Mock
    private RankingTypeRepository rankingTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private StudyTimeRankingService studyTimeRankingService;

    @Test
    @DisplayName("getRankings는 type 공백을 제거하고 DAILY 정렬로 랭킹을 조회한다")
    void getRankingsFetchesDailyRankingsWithTrimmedType() {
        // given
        User user = createUserWithIds(10, 3, "2021136001");
        RankingType rankingType = rankingType(5, "CLUB");
        StudyTimeRanking first = ranking(5, 3, 100, "Alpha", 500L, 2000L);
        StudyTimeRanking second = ranking(5, 3, 101, "Beta", 400L, 1900L);
        StudyTimeRankingCondition condition = new StudyTimeRankingCondition(2, 2, " CLUB ", StudyTimeRankingSort.DAILY);
        PageRequest pageRequest = PageRequest.of(1, 2);

        given(userRepository.getById(10)).willReturn(user);
        given(rankingTypeRepository.getByNameIgnoreCase("CLUB")).willReturn(rankingType);
        given(studyTimeRankingRepository.findDailyRankings(5, 3, pageRequest))
            .willReturn(new PageImpl<>(List.of(first, second), pageRequest, 5));

        // when
        StudyTimeRankingsResponse response = studyTimeRankingService.getRankings(condition, 10);

        // then
        assertThat(response.totalCount()).isEqualTo(5);
        assertThat(response.currentPage()).isEqualTo(2);
        assertThat(response.rankings())
            .extracting(ranking -> ranking.rank(), ranking -> ranking.name())
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(3, "Alpha"),
                org.assertj.core.groups.Tuple.tuple(4, "Beta")
            );
        verify(studyTimeRankingRepository).findDailyRankings(5, 3, pageRequest);
    }

    @Test
    @DisplayName("getMyRankings는 랭킹이 없는 동아리를 제외하고 rank 순으로 정렬한다")
    void getMyRankingsFiltersMissingClubRankingsAndSortsByRank() {
        // given
        User user = createUserWithIds(1, 7, "2021136001");
        Club firstClub = createClubWithId(20, 7, "첫 번째 동아리");
        Club secondClub = createClubWithId(30, 7, "두 번째 동아리");
        ClubMember firstMember = ClubMemberFixture.createMember(firstClub, user);
        ClubMember secondMember = ClubMemberFixture.createMember(secondClub, user);
        RankingType clubType = rankingType(101, "CLUB");
        RankingType studentType = rankingType(102, "STUDENT_NUMBER");
        RankingType personalType = rankingType(103, "PERSONAL");
        StudyTimeRanking clubRanking = ranking(101, 7, 30, "두 번째 동아리", 120L, 1000L);
        StudyTimeRanking studentRanking = ranking(102, 7, 1, "2021", 80L, 900L);
        StudyTimeRanking personalRanking = ranking(103, 7, 1, "홍길동", 70L, 800L);

        given(userRepository.getById(1)).willReturn(user);
        given(rankingTypeRepository.getByNameIgnoreCase("CLUB")).willReturn(clubType);
        given(rankingTypeRepository.getByNameIgnoreCase("STUDENT_NUMBER")).willReturn(studentType);
        given(rankingTypeRepository.getByNameIgnoreCase("PERSONAL")).willReturn(personalType);
        given(clubMemberRepository.findByUserId(1)).willReturn(List.of(secondMember, firstMember));
        given(studyTimeRankingRepository.findRanking(101, 7, 20)).willReturn(Optional.empty());
        given(studyTimeRankingRepository.findRanking(101, 7, 30)).willReturn(Optional.of(clubRanking));
        given(studyTimeRankingRepository.countMonthlyHigherRankings(101, 7, 1000L, 120L, 30)).willReturn(0L);
        given(studyTimeRankingRepository.findRankingByName(102, 7, "2021")).willReturn(Optional.of(studentRanking));
        given(studyTimeRankingRepository.countMonthlyHigherRankings(102, 7, 900L, 80L, 1)).willReturn(1L);
        given(studyTimeRankingRepository.findRanking(103, 7, 1)).willReturn(Optional.of(personalRanking));
        given(studyTimeRankingRepository.countMonthlyHigherRankings(103, 7, 800L, 70L, 1)).willReturn(2L);

        // when
        StudyTimeMyRankingsResponse response = studyTimeRankingService.getMyRankings(
            new StudyTimeMyRankingCondition(StudyTimeRankingSort.MONTHLY),
            1
        );

        // then
        assertThat(response.clubRankings())
            .extracting(ranking -> ranking.rank(), ranking -> ranking.name())
            .containsExactly(org.assertj.core.groups.Tuple.tuple(1, "두 번째 동아리"));
        assertThat(response.studentNumberRanking().rank()).isEqualTo(2);
        assertThat(response.studentNumberRanking().name()).isEqualTo("21");
        assertThat(response.personalRanking().rank()).isEqualTo(3);
        assertThat(response.personalRanking().name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("getMyRankings는 DAILY 정렬에서 daily 우선 순위 계산을 사용한다")
    void getMyRankingsUsesDailyRankCalculationWhenSortIsDaily() {
        // given
        User user = createUserWithIds(2, 9, "2022136001");
        RankingType clubType = rankingType(201, "CLUB");
        RankingType studentType = rankingType(202, "STUDENT_NUMBER");
        RankingType personalType = rankingType(203, "PERSONAL");
        StudyTimeRanking studentRanking = ranking(202, 9, 2, "2022", 300L, 1000L);

        given(userRepository.getById(2)).willReturn(user);
        given(rankingTypeRepository.getByNameIgnoreCase("CLUB")).willReturn(clubType);
        given(rankingTypeRepository.getByNameIgnoreCase("STUDENT_NUMBER")).willReturn(studentType);
        given(rankingTypeRepository.getByNameIgnoreCase("PERSONAL")).willReturn(personalType);
        given(clubMemberRepository.findByUserId(2)).willReturn(List.of());
        given(studyTimeRankingRepository.findRankingByName(202, 9, "2022")).willReturn(Optional.of(studentRanking));
        given(studyTimeRankingRepository.countDailyHigherRankings(202, 9, 300L, 1000L, 2)).willReturn(4L);
        given(studyTimeRankingRepository.findRanking(203, 9, 2)).willReturn(Optional.empty());

        // when
        StudyTimeMyRankingsResponse response = studyTimeRankingService.getMyRankings(
            new StudyTimeMyRankingCondition(StudyTimeRankingSort.DAILY),
            2
        );

        // then
        assertThat(response.clubRankings()).isEmpty();
        assertThat(response.studentNumberRanking().rank()).isEqualTo(5);
        assertThat(response.personalRanking()).isNull();
        verify(studyTimeRankingRepository).countDailyHigherRankings(202, 9, 300L, 1000L, 2);
    }

    private User createUserWithIds(Integer userId, Integer universityId, String studentNumber) {
        var university = UniversityFixture.create();
        ReflectionTestUtils.setField(university, "id", universityId);

        User user = UserFixture.createUser(university, "테스트유저", studentNumber);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Club createClubWithId(Integer clubId, Integer universityId, String name) {
        var university = UniversityFixture.create();
        ReflectionTestUtils.setField(university, "id", universityId);

        Club club = ClubFixture.create(university, name);
        ReflectionTestUtils.setField(club, "id", clubId);
        return club;
    }

    private RankingType rankingType(Integer id, String name) {
        RankingType rankingType = mock(RankingType.class);
        given(rankingType.getId()).willReturn(id);
        return rankingType;
    }

    private StudyTimeRanking ranking(
        Integer rankingTypeId,
        Integer universityId,
        Integer targetId,
        String targetName,
        Long dailySeconds,
        Long monthlySeconds
    ) {
        StudyTimeRanking ranking = StudyTimeRanking.builder()
            .id(gg.agit.konect.domain.studytime.model.StudyTimeRankingId.builder()
                .rankingTypeId(rankingTypeId)
                .universityId(universityId)
                .targetId(targetId)
                .build())
            .targetName(targetName)
            .dailySeconds(dailySeconds)
            .monthlySeconds(monthlySeconds)
            .build();
        return ranking;
    }
}
