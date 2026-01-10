package gg.agit.konect.domain.studytime.model;

import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ranking_type")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class RankingType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    public static final String RANKING_TYPE_CLUB = "CLUB";
    public static final String RANKING_TYPE_STUDENT_NUMBER = "STUDENT_NUMBER";
    public static final String RANKING_TYPE_PERSONAL = "PERSONAL";
}
