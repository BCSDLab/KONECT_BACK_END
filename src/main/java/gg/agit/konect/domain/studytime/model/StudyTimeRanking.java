package gg.agit.konect.domain.studytime.model;

import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "study_time_ranking")
@NoArgsConstructor(access = PROTECTED)
public class StudyTimeRanking extends BaseEntity {

    @EmbeddedId
    private StudyTimeRankingId id;

    @NotNull
    @Column(name = "target_name", nullable = false, length = 100)
    private String targetName;

    @NotNull
    @Column(name = "daily_seconds", nullable = false)
    private Long dailySeconds;

    @NotNull
    @Column(name = "monthly_seconds", nullable = false)
    private Long monthlySeconds;

    @Builder
    private StudyTimeRanking(
        StudyTimeRankingId id,
        String targetName,
        Long dailySeconds,
        Long monthlySeconds
    ) {
        this.id = id;
        this.targetName = targetName;
        this.dailySeconds = dailySeconds;
        this.monthlySeconds = monthlySeconds;
    }

    public void updateSeconds(Long dailySeconds, Long monthlySeconds) {
        this.dailySeconds = dailySeconds;
        this.monthlySeconds = monthlySeconds;
    }
}
