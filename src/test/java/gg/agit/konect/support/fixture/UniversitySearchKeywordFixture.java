package gg.agit.konect.support.fixture;

import java.util.Locale;

import gg.agit.konect.domain.university.enums.UniversitySearchKeywordType;
import gg.agit.konect.domain.university.model.UniversitySearchKeyword;
import gg.agit.konect.domain.website.model.WebUniversity;

public class UniversitySearchKeywordFixture {

    public static UniversitySearchKeyword createAlias(WebUniversity university, String keyword) {
        return UniversitySearchKeyword.builder()
            .university(university)
            .keyword(keyword)
            .normalizedKeyword(keyword.replaceAll("\\s", "").toLowerCase(Locale.ROOT))
            .keywordType(UniversitySearchKeywordType.ALIAS)
            .build();
    }
}
