package gg.agit.konect.club.enums;

import java.time.LocalDate;

import gg.agit.konect.club.model.ClubRecruitment;
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

        LocalDate now = LocalDate.now();

        if (now.isBefore(clubRecruitment.getStartDate())) {
            return BEFORE;
        }

        if (now.isAfter(clubRecruitment.getEndDate())) {
            return CLOSED;
        }

        return ONGOING;
    }
}
