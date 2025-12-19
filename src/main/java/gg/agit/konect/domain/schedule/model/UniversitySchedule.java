package gg.agit.konect.domain.schedule.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.global.model.BaseEntity;
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
@Table(name = "university_schedule")
@NoArgsConstructor(access = PROTECTED)
public class UniversitySchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @NotNull
    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Builder
    private UniversitySchedule(
        Integer id,
        University university,
        String title,
        LocalDateTime startedAt,
        LocalDateTime endedAt
    ) {
        this.id = id;
        this.university = university;
        this.title = title;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public Integer calculateDDay(LocalDate today) {
        if (today.isBefore(this.startedAt.toLocalDate())) {
            return (int)ChronoUnit.DAYS.between(today, this.startedAt.toLocalDate());
        }
        return null;
    }
}
