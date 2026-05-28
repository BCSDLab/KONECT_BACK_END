package gg.agit.konect.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.model.WebUniversity;

public class WebClubFixture {

    public static WebClub create(WebUniversity university) {
        return create(university, "BCSD Lab", ClubCategory.ACADEMIC);
    }

    public static WebClub create(WebUniversity university, String name, ClubCategory category) {
        return WebClub.builder()
            .university(university)
            .name(name)
            .description("한 줄 소개")
            .introduce("상세 소개입니다.")
            .imageUrl("https://example.com/" + name + ".png")
            .clubCategory(category)
            .topic("코딩")
            .build();
    }

    public static WebClub createWithId(Integer id, WebUniversity university) {
        WebClub club = create(university);
        ReflectionTestUtils.setField(club, "id", id);
        return club;
    }

    public static WebClub createWithId(Integer id, WebUniversity university, String name, ClubCategory category) {
        WebClub club = create(university, name, category);
        ReflectionTestUtils.setField(club, "id", id);
        return club;
    }
}
