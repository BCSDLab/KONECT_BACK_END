package gg.agit.konect.domain.schedule.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "university_schedule")
@NoArgsConstructor(access = PROTECTED)
public class UniversitySchedule extends BaseEntity {

    @Id
    private Integer id;

    @MapsId
    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "id", nullable = false, updatable = false, unique = true)
    private Schedule schedule;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Builder
    private UniversitySchedule(
        Schedule schedule,
        University university
    ) {
        this.schedule = schedule;
        this.university = university;
    }
}
