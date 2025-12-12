package gg.agit.konect.university.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Campus {
    MAIN("본교"),
    SECOND("제2캠퍼스"),
    THIRD("제3캠퍼스"),
    ;

    private final String displayName;
}
