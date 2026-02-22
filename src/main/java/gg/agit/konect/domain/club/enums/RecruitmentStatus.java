package gg.agit.konect.domain.club.enums;

import java.time.LocalDateTime;

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

        LocalDateTime startDate = clubRecruitment.getStartDate();
        LocalDateTime endDate = clubRecruitment.getEndDate();

        if (startDate == null || endDate == null) {
            return CLOSED;
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startDate)) {
            return BEFORE;
        }

        if (now.isAfter(endDate)) {
            return CLOSED;
        }

        return ONGOING;
    }
}
