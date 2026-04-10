package gg.agit.konect.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.model.University;

public class ClubFixture {

    public static Club create(University university) {
        return create(university, "BCSD Lab");
    }

    public static Club create(University university, String name) {
        return Club.builder()
            .university(university)
            .name(name)
            .description("테스트 동아리 소개")
            .introduce("테스트 동아리 상세 소개입니다.")
            .imageUrl("https://example.com/club.png")
            .location("학생회관 101호")
            .clubCategory(ClubCategory.ACADEMIC)
            .isRecruitmentEnabled(false)
            .isApplicationEnabled(true)
            .isFeeRequired(false)
            .build();
    }

    public static Club createWithId(University university, Integer id) {
        return createWithId(university, id, "BCSD Lab");
    }

    public static Club createWithId(University university, Integer id, String name) {
        Club club = create(university, name);
        ReflectionTestUtils.setField(club, "id", id);
        return club;
    }

    public static Club createWithRecruitment(University university, String name) {
        return Club.builder()
            .university(university)
            .name(name)
            .description("테스트 동아리 소개")
            .introduce("테스트 동아리 상세 소개입니다.")
            .imageUrl("https://example.com/club.png")
            .location("학생회관 101호")
            .clubCategory(ClubCategory.ACADEMIC)
            .isRecruitmentEnabled(true)
            .isApplicationEnabled(true)
            .isFeeRequired(false)
            .build();
    }
}
