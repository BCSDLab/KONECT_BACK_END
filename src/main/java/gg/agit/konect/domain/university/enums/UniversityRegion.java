package gg.agit.konect.domain.university.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UniversityRegion {

    SEOUL("서울"),
    GYEONGGI("경기도"),
    CHUNGCHEONG("충청도"),
    JEOLLA("전라도"),
    GYEONGSANG("경상도"),
    GANGWON("강원도"),
    JEJU("제주도"),
    UNKNOWN("지역 미지정"),
    ;

    private final String displayName;

    public static List<UniversityRegion> sortedForDisplay() {
        return Arrays.stream(values())
            // 지역 미지정은 실제 지역 필터가 아니므로 가나다 정렬과 무관하게 마지막에 노출한다.
            .sorted(Comparator.comparing(UniversityRegion::isUnknown)
                .thenComparing(UniversityRegion::getDisplayName))
            .toList();
    }

    private boolean isUnknown() {
        return this == UNKNOWN;
    }
}
