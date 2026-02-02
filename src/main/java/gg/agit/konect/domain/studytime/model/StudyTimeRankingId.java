package gg.agit.konect.domain.studytime.model;

import static lombok.AccessLevel.PROTECTED;

import java.io.Serializable;

import gg.agit.konect.domain.university.model.University;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class StudyTimeRankingId implements Serializable {

    @Column(name = "ranking_type_id", nullable = false)
    private Integer rankingTypeId;

    @Column(name = "university_id", nullable = false)
    private Integer universityId;

    @Column(name = "target_id", nullable = false)
    private Integer targetId;

    @Builder
    private StudyTimeRankingId(Integer rankingTypeId, Integer universityId, Integer targetId) {
        this.rankingTypeId = rankingTypeId;
        this.universityId = universityId;
        this.targetId = targetId;
    }

    public static StudyTimeRankingId of(RankingType rankingType, University university, Integer targetId) {
        return StudyTimeRankingId.builder()
            .rankingTypeId(rankingType.getId())
            .universityId(university.getId())
            .targetId(targetId)
            .build();
    }
}
