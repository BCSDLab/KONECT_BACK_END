package gg.agit.konect.domain.studytime.model;

import static lombok.AccessLevel.PROTECTED;

import java.io.Serializable;

import gg.agit.konect.domain.studytime.enums.StudyTimeRankingType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class StudyTimeRankingId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_type", nullable = false, length = 20)
    private StudyTimeRankingType rankingType;

    @Column(name = "target_id", nullable = false, length = 20)
    private String targetId;

    @Builder
    private StudyTimeRankingId(StudyTimeRankingType rankingType, String targetId) {
        this.rankingType = rankingType;
        this.targetId = targetId;
    }
}
