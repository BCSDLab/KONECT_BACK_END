package gg.agit.konect.domain.schedule.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.InheritanceType.JOINED;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "schedule")
@Inheritance(strategy = JOINED)
@DiscriminatorColumn(name = "schedule_type")
@NoArgsConstructor(access = PROTECTED)
public abstract class Schedule extends BaseEntity {

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

    protected Schedule(Integer id, String title, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.id = id;
        this.title = title;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public Integer calculateDDay(LocalDate today) {
        if (today.isBefore(this.startedAt.toLocalDate())) {
            return (int) ChronoUnit.DAYS.between(today, this.startedAt.toLocalDate());
        }
        return null;
    }
}
