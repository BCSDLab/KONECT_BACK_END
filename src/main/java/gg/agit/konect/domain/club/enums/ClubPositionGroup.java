package gg.agit.konect.domain.club.enums;

import lombok.Getter;

@Getter
public enum ClubPositionGroup {
    PRESIDENT("회장"),
    MANAGER("운영진"),
    MEMBER("일반회원"),
    ;

    private final String description;

    ClubPositionGroup(String description) {
        this.description = description;
    }
}
