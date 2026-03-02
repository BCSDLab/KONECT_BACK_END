package gg.agit.konect.support.fixture;

import java.time.LocalDateTime;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;

public class ClubRecruitmentFixture {

    private static final int RECRUITMENT_PERIOD_DAYS = 30;

    public static ClubRecruitment createAlwaysRecruiting(Club club) {
        return ClubRecruitment.of(
            null,
            null,
            true,
            "상시 모집 공고 내용입니다.",
            club
        );
    }

    public static ClubRecruitment createWithPeriod(Club club, LocalDateTime startAt, LocalDateTime endAt) {
        return ClubRecruitment.of(
            startAt,
            endAt,
            false,
            "기간 모집 공고 내용입니다.",
            club
        );
    }

    public static ClubRecruitment createCurrentlyRecruiting(Club club) {
        LocalDateTime now = LocalDateTime.now();
        return createWithPeriod(club, now.minusDays(1), now.plusDays(RECRUITMENT_PERIOD_DAYS));
    }
}
