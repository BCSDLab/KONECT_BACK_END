package gg.agit.konect.domain.upload.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadTarget {
    CLUB("동아리"),
    BANK("은행"),
    COUNCIL("총학생회"),
    USER("사용자"),
    ;

    private final String description;
}
