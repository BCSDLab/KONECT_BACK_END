package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.model.University;

public class UniversityFixture {

    public static University create() {
        return create("한국기술교육대학교", Campus.MAIN);
    }

    public static University create(String koreanName, Campus campus) {
        return University.builder()
            .koreanName(koreanName)
            .campus(campus)
            .build();
    }
}
