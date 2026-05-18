package gg.agit.konect.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.university.model.University;

public class UniversityFixture {

    private static final String DEFAULT_IMAGE_URL =
        "https://stage-static.koreatech.in/konect/user/university_logo_sample.png";

    public static University create() {
        return create("한국기술교육대학교", Campus.MAIN);
    }

    public static University create(String koreanName, Campus campus) {
        return create(koreanName, campus, UniversityRegion.CHUNGCHEONG);
    }

    public static University create(String koreanName, Campus campus, UniversityRegion region) {
        return create(koreanName, campus, region, DEFAULT_IMAGE_URL);
    }

    public static University create(String koreanName, Campus campus, UniversityRegion region, String imageUrl) {
        return University.builder()
            .koreanName(koreanName)
            .campus(campus)
            .region(region)
            .imageUrl(imageUrl)
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
