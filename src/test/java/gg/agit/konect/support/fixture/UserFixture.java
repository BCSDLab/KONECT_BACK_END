package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;

public class UserFixture {

    public static User createUser(University university) {
        return createUser(university, "테스트유저", "2021136001");
    }

    public static User createUser(University university, String name, String studentNumber) {
        return User.builder()
            .university(university)
            .email(studentNumber + "@koreatech.ac.kr")
            .name(name)
            .studentNumber(studentNumber)
            .role(UserRole.USER)
            .isMarketingAgreement(true)
            .imageUrl("https://example.com/profile.png")
            .build();
    }

    public static User createAdmin(University university) {
        return User.builder()
            .university(university)
            .email("admin@koreatech.ac.kr")
            .name("관리자")
            .studentNumber("2020000001")
            .role(UserRole.ADMIN)
            .isMarketingAgreement(true)
            .imageUrl("https://example.com/admin.png")
            .build();
    }
}
