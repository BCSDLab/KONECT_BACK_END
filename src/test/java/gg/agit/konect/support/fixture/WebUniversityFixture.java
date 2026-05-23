package gg.agit.konect.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.web.model.WebUniversity;

public class WebUniversityFixture {

    private static final String DEFAULT_IMAGE_URL =
        "https://stage-static.koreatech.in/konect/university/university_logo_sample.webp";

    public static WebUniversity create() {
        return create("한국기술교육대학교", Campus.MAIN);
    }

    public static WebUniversity create(String koreanName, Campus campus) {
        return create(koreanName, campus, UniversityRegion.CHUNGCHEONG);
    }

    public static WebUniversity create(String koreanName, Campus campus, UniversityRegion region) {
        return create(koreanName, campus, region, DEFAULT_IMAGE_URL);
    }

    public static WebUniversity create(String koreanName, Campus campus, UniversityRegion region, String imageUrl) {
        return WebUniversity.builder()
            .koreanName(koreanName)
            .campus(campus)
            .region(region)
            .imageUrl(imageUrl)
            .build();
    }

    public static WebUniversity createWithName(String koreanName) {
        return create(koreanName, Campus.MAIN);
    }

    public static WebUniversity createWithId(Integer id) {
        return createWithId(id, "한국기술교육대학교", Campus.MAIN);
    }

    public static WebUniversity createWithId(Integer id, String koreanName, Campus campus) {
        WebUniversity university = create(koreanName, campus);
        ReflectionTestUtils.setField(university, "id", id);
        return university;
    }
}
