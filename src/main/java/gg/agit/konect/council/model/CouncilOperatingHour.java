package gg.agit.konect.council.model;

import static gg.agit.konect.global.code.ApiResponseCode.*;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.DayOfWeek;
import java.time.LocalTime;

import gg.agit.konect.common.model.BaseEntity;
import gg.agit.konect.global.exception.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "council_operating_hour")
@NoArgsConstructor(access = PROTECTED)
public class CouncilOperatingHour extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @NotNull
    @Column(name = "day_of_week", length = 20, nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @Builder
    private CouncilOperatingHour(
        Integer id,
        Council council,
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean isClosed
    ) {
        this.id = id;
        this.council = council;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isClosed = isClosed;
    }

    public void validate() {
        if (isClosed) {
            validateClosedDay();
        } else {
            validateOpenDay();
        }
    }

    private void validateClosedDay() {
        if (openTime != null || closeTime != null) {
            throw CustomException.of(INVALID_OPERATING_HOURS_CLOSED);
        }
    }

    private void validateOpenDay() {
        if (openTime == null || closeTime == null) {
            throw CustomException.of(INVALID_OPERATING_HOURS_TIME);
        }
        if (!openTime.isBefore(closeTime)) {
            throw CustomException.of(INVALID_OPERATING_HOURS_TIME);
        }
    }
}
