package gg.agit.konect.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.studytime.model.RankingType;

public class RankingTypeFixture {

    public static RankingType createWithId(Integer id) {
        RankingType rankingType = new TestRankingType();
        ReflectionTestUtils.setField(rankingType, "id", id);
        return rankingType;
    }

    private static class TestRankingType extends RankingType {
    }
}
