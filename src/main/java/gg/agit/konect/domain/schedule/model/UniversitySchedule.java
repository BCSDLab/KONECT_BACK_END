package gg.agit.konect.domain.schedule.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.university.model.University;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "university_schedule")
@DiscriminatorValue("UNIVERSITY")
@NoArgsConstructor(access = PROTECTED)
public class UniversitySchedule extends Schedule {

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Builder
    private UniversitySchedule(
        Integer id,
        String title,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        University university
    ) {
        super(id, title, startedAt, endedAt);
        this.university = university;
    }
}
