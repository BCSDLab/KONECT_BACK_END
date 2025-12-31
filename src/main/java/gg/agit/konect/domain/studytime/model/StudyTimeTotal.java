package gg.agit.konect.domain.studytime.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "study_time_total",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_study_time_total_user", columnNames = {"user_id"})
    }
)
@NoArgsConstructor(access = PROTECTED)
public class StudyTimeTotal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Column(name = "total_seconds", nullable = false)
    private Long totalSeconds;

    @Builder
    private StudyTimeTotal(User user, Long totalSeconds) {
        this.user = user;
        this.totalSeconds = totalSeconds;
    }

    public void addSeconds(long seconds) {
        this.totalSeconds += seconds;
    }
}
