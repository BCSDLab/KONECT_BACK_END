package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.council.model.Council;
import gg.agit.konect.domain.university.model.University;

public class CouncilFixture {

    public static Council create(University university) {
        return Council.builder()
            .name("총학생회")
            .imageUrl("https://example.com/council.png")
            .introduce("학생회 소개입니다.")
            .location("학생회관 301호")
            .personalColor("#FF5733")
            .phoneNumber("041-560-1234")
            .email("council@koreatech.ac.kr")
            .instagramUserName("koreatech_council")
            .operatingHour("09:00 - 18:00")
            .university(university)
            .build();
    }
}
