package gg.agit.konect.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.university.model.University;

public class UniversityFixture {

    public static University create() {
        return create("한국기술교육대학교", Campus.MAIN);
    }

    public static University create(String koreanName, Campus campus) {
        return create(koreanName, campus, UniversityRegion.CHUNGCHEONG);
    }

    public static University create(String koreanName, Campus campus, UniversityRegion region) {
        return create(koreanName, campus, region, null);
    }

    public static University create(String koreanName, Campus campus, UniversityRegion region, String logoImageUrl) {
        return University.builder()
            .koreanName(koreanName)
            .campus(campus)
            .region(region)
            .logoImageUrl(logoImageUrl)
            .build();
    }

    public static University createWithName(String koreanName) {
        return create(koreanName, Campus.MAIN);
    }

    public static University createWithId(Integer id) {
        return createWithId(id, "한국기술교육대학교", Campus.MAIN);
    }

    public static University createWithId(Integer id, String koreanName, Campus campus) {
        University university = create(koreanName, campus);
        ReflectionTestUtils.setField(university, "id", id);
        return university;
    }
}
