package gg.agit.konect.domain.schedule.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "schedule")
@NoArgsConstructor(access = PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @NotNull
    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Builder
    private Schedule(
        Integer id,
        String title,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        ScheduleType scheduleType
    ) {
        this.id = id;
        this.title = title;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.scheduleType = scheduleType;
    }

    public static Schedule of(
        String title,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        ScheduleType scheduleType
    ) {
        return Schedule.builder()
            .title(title)
            .startedAt(startedAt)
            .endedAt(endedAt)
            .scheduleType(scheduleType)
            .build();
    }

    public void update(
        String title,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        ScheduleType scheduleType
    ) {
        this.title = title;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.scheduleType = scheduleType;
    }

    public Integer calculateDDay(LocalDate today) {
        if (today.isBefore(this.startedAt.toLocalDate())) {
            return (int)ChronoUnit.DAYS.between(today, this.startedAt.toLocalDate());
        }
        return null;
    }
}
