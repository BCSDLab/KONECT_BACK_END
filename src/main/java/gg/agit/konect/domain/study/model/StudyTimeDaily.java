package gg.agit.konect.domain.study.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "study_time_daily",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_study_time_daily_user_date", columnNames = {"user_id", "study_date"})
    }
)
@NoArgsConstructor(access = PROTECTED)
public class StudyTimeDaily extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @NotNull
    @Column(name = "total_seconds", nullable = false)
    private Long totalSeconds;

    @Builder
    private StudyTimeDaily(User user, LocalDate studyDate, Long totalSeconds) {
        this.user = user;
        this.studyDate = studyDate;
        this.totalSeconds = totalSeconds;
    }

    public void addSeconds(long seconds) {
        this.totalSeconds += seconds;
    }
}
