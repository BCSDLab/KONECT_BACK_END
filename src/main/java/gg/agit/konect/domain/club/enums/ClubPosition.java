package gg.agit.konect.domain.club.enums;

import java.util.EnumSet;
import java.util.Set;

import lombok.Getter;

@Getter
public enum ClubPosition {
    PRESIDENT("회장", 0),
    VICE_PRESIDENT("부회장", 1),
    MANAGER("운영진", 2),
    MEMBER("일반회원", 3),
    ;

    private final String description;
    private final int priority;

    ClubPosition(String description, int priority) {
        this.description = description;
        this.priority = priority;
    }

    public boolean canManage(ClubPosition target) {
        return this.priority < target.priority;
    }

    public boolean isHigherThan(ClubPosition target) {
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

    public static final Set<ClubPosition> PRESIDENT_ONLY = EnumSet.of(PRESIDENT);
    public static final Set<ClubPosition> LEADERS = EnumSet.of(PRESIDENT, VICE_PRESIDENT);
    public static final Set<ClubPosition> MANAGERS = EnumSet.of(PRESIDENT, VICE_PRESIDENT, MANAGER);
}
