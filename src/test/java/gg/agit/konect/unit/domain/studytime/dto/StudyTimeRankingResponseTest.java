package gg.agit.konect.unit.domain.studytime.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.studytime.dto.StudyTimeRankingResponse;
import gg.agit.konect.domain.studytime.model.RankingType;
import gg.agit.konect.domain.studytime.model.StudyTimeRanking;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;

class StudyTimeRankingResponseTest extends ServiceTestSupport {

    @Test
    @DisplayName("개인 랭킹 이름은 첫 글자와 마지막 글자만 남기고 마스킹한다")
    void fromMasksPersonalRankingName() {
        // given
        StudyTimeRanking ranking = createRanking("김민수");

        // when
        StudyTimeRankingResponse response = StudyTimeRankingResponse.from(ranking, 1, "PERSONAL");

        // then
        assertThat(response.name()).isEqualTo("김*수");
    }

    @Test
    @DisplayName("두 글자 개인 이름은 첫 글자만 남기고 마스킹한다")
    void fromMasksTwoLetterPersonalRankingName() {
        // given
        StudyTimeRanking ranking = createRanking("길동");

        // when
        StudyTimeRankingResponse response = StudyTimeRankingResponse.from(ranking, 1, "PERSONAL");

        // then
        assertThat(response.name()).isEqualTo("길*");
    }

    @Test
    @DisplayName("학번 랭킹 이름은 입학연도 뒤 두 자리만 노출한다")
    void fromDisplaysLastTwoDigitsForStudentNumberRanking() {
        // given
        StudyTimeRanking ranking = createRanking("2024");

        // when
        StudyTimeRankingResponse response = StudyTimeRankingResponse.from(ranking, 3, "STUDENT_NUMBER");

        // then
        assertThat(response.name()).isEqualTo("24");
        assertThat(response.rank()).isEqualTo(3);
    }

    @Test
    @DisplayName("동아리 랭킹 이름은 원문을 유지한다")
    void fromKeepsClubRankingName() {
        // given
        StudyTimeRanking ranking = createRanking("BCSD Lab");

        // when
        StudyTimeRankingResponse response = StudyTimeRankingResponse.from(ranking, 2, "CLUB");

        // then
        assertThat(response.name()).isEqualTo("BCSD Lab");
    }

    private StudyTimeRanking createRanking(String targetName) {
        RankingType rankingType = new TestRankingType();
        ReflectionTestUtils.setField(rankingType, "id", 1);

        University university = UniversityFixture.createWithId(1);

        return StudyTimeRanking.of(rankingType, university, 10, targetName, 100L, 1000L);
    }

    private static class TestRankingType extends RankingType {
    }
}
