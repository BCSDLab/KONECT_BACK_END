package gg.agit.konect.domain.university.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.university.enums.Campus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "university",
    uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(
            name = "uq_university_korean_name_campus",
            columnNames = {"korean_name", "campus"}
        ),
    })
@NoArgsConstructor(access = PROTECTED)
public class University {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "korean_name", nullable = false)
    private String koreanName;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "campus", nullable = false)
    private Campus campus;

    @Builder
    private University(Integer id, String koreanName, Campus campus) {
        this.id = id;
        this.koreanName = koreanName;
        this.campus = campus;
    }
}
