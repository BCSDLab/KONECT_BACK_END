package gg.agit.konect.domain.club.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ClubCategory {
    PERFORMANCE("공연", 1),
    SOCIAL_SERVICE("사회/봉사", 2),
    EXHIBITION_CREATION("전시/창작", 3),
    RELIGION("종교", 4),
    SPORTS("체육(운동)", 5),
    HOBBY("취미", 6),
    ACADEMIC("학술", 7),
    ETC("기타", 8);

    @Getter
    private final String description;
    private final int displayOrder;

    public static List<ClubCategory> sortedForDisplay() {
        return Arrays.stream(values())
            .sorted(Comparator.comparingInt(category -> category.displayOrder))
            .toList();
    }
}
