package gg.agit.konect.domain.club.enums;

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
    JUNIOR("준동아리")
    ;

    private final String description;
}
