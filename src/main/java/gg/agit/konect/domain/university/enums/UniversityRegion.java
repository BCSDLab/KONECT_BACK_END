package gg.agit.konect.domain.university.enums;

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
}
