package gg.agit.konect.domain.club.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClubCategory {
    ACADEMIC("학술"),
    SPORTS("운동"),
    HOBBY("취미"),
    RELIGION("종교"),
    PERFORMANCE("공연"),
    JUNIOR("준동아리");

    private final String description;

    public static List<ClubCategory> sortedForDisplay() {
        return Arrays.stream(values())
            // 준동아리는 정식 동아리 분과가 아니므로 가나다 정렬과 무관하게 마지막에 노출한다.
            .sorted(Comparator.comparing(ClubCategory::isJunior)
                .thenComparing(ClubCategory::getDescription))
            .toList();
    }

    private boolean isJunior() {
        return this == JUNIOR;
    }
}
