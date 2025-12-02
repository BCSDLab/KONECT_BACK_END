package gg.agit.konect.club.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeePaymentStatus {

    UNPAID("미납"),
    PAID("납부"),
    EXEMPT("면제"),
    ;

    private final String description;
}
