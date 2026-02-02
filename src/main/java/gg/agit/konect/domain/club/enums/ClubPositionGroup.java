package gg.agit.konect.domain.club.enums;

import java.util.EnumSet;
import java.util.Set;

import lombok.Getter;

@Getter
public enum ClubPositionGroup {
    PRESIDENT("회장", 0, 1, 1),
    VICE_PRESIDENT("부회장", 1, 0, 1),
    MANAGER("운영진", 2, 0, 20),
    MEMBER("일반회원", 3, 0, Integer.MAX_VALUE),
    ;

    private final String description;
    private final int priority;
    private final int minCount;
    private final int maxCount;

    ClubPositionGroup(String description, int priority, int minCount, int maxCount) {
        this.description = description;
        this.priority = priority;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public boolean canManage(ClubPositionGroup target) {
        return this.priority < target.priority;
    }

    public boolean isHigherThan(ClubPositionGroup target) {
        return this.priority < target.priority;
    }

    public boolean isPresident() {
        return this == PRESIDENT;
    }

    public boolean isVicePresident() {
        return this == VICE_PRESIDENT;
    }

    public boolean isManager() {
        return this == MANAGER;
    }

    public boolean isMember() {
        return this == MEMBER;
    }

    public static final Set<ClubPositionGroup> PRESIDENT_ONLY = EnumSet.of(PRESIDENT);
    public static final Set<ClubPositionGroup> LEADERS = EnumSet.of(PRESIDENT, VICE_PRESIDENT);
    public static final Set<ClubPositionGroup> MANAGERS = EnumSet.of(PRESIDENT, VICE_PRESIDENT, MANAGER);
}
