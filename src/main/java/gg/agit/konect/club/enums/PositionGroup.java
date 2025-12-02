package gg.agit.konect.club.enums;

import lombok.Getter;

@Getter
public enum PositionGroup {
    PRESIDENT("회장"),
    MANAGER("운영진"),
    MEMBER("일반회원"),
    ;

    private final String description;

    PositionGroup(String description) {
        this.description = description;
    }
}
