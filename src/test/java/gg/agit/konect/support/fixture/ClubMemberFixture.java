package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.user.model.User;

public class ClubMemberFixture {

    public static ClubMember createPresident(Club club, User user) {
        return ClubMember.builder()
            .club(club)
            .user(user)
            .clubPosition(ClubPosition.PRESIDENT)
            .build();
    }

    public static ClubMember createVicePresident(Club club, User user) {
        return ClubMember.builder()
            .club(club)
            .user(user)
            .clubPosition(ClubPosition.VICE_PRESIDENT)
            .build();
    }

    public static ClubMember createManager(Club club, User user) {
        return ClubMember.builder()
            .club(club)
            .user(user)
            .clubPosition(ClubPosition.MANAGER)
            .build();
    }

    public static ClubMember createMember(Club club, User user) {
        return ClubMember.builder()
            .club(club)
            .user(user)
            .clubPosition(ClubPosition.MEMBER)
            .build();
    }
}
