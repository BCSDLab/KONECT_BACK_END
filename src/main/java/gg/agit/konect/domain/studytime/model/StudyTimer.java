package gg.agit.konect.domain.studytime.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "study_timer")
@NoArgsConstructor(access = PROTECTED)
public class StudyTimer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Builder
    private StudyTimer(User user, LocalDateTime startedAt) {
        this.user = user;
        this.startedAt = startedAt;
    }

    public static StudyTimer of(User user, LocalDateTime startedAt) {
        return StudyTimer.builder()
            .user(user)
            .startedAt(startedAt)
            .build();
    }

    public void updateStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
}
