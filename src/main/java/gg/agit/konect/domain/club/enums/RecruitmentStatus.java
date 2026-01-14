package gg.agit.konect.domain.club.enums;

import java.time.LocalDate;

import gg.agit.konect.domain.club.model.ClubRecruitment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecruitmentStatus {

    BEFORE("모집 전"),
    ONGOING("모집 중"),
    CLOSED("모집 마감");

    private final String description;

    public static RecruitmentStatus of(ClubRecruitment clubRecruitment) {
        if (clubRecruitment == null) {
            return CLOSED;
        }

        if (Boolean.TRUE.equals(clubRecruitment.getIsAlwaysRecruiting())) {
            return ONGOING;
        }

        LocalDate startDate = clubRecruitment.getStartDate();
        LocalDate endDate = clubRecruitment.getEndDate();

        if (startDate == null || endDate == null) {
            return CLOSED;
        }

        LocalDate now = LocalDate.now();

        if (now.isBefore(startDate)) {
            return BEFORE;
        }

        if (now.isAfter(endDate)) {
            return CLOSED;
        }

        return ONGOING;
    }
}
