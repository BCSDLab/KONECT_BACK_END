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

        LocalDateTime startAt = clubRecruitment.getStartAt();
        LocalDateTime endAt = clubRecruitment.getEndAt();

        if (startAt == null || endAt == null) {
            return CLOSED;
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startAt)) {
            return BEFORE;
        }

        if (now.isAfter(endAt)) {
            return CLOSED;
        }

        return ONGOING;
    }
}
