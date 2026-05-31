package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.university.enums.UniversitySearchKeywordType;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.university.model.UniversitySearchKeyword;

public class UniversitySearchKeywordFixture {

    public static UniversitySearchKeyword createAlias(University university, String keyword) {
        return UniversitySearchKeyword.builder()
            .university(university)
            .keyword(keyword)
            .normalizedKeyword(keyword.replaceAll("\\s", "").toLowerCase())
            .keywordType(UniversitySearchKeywordType.ALIAS)
            .build();
    }
}
