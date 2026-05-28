package gg.agit.konect.domain.website.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "web_university",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_web_university_korean_name_campus",
            columnNames = {"korean_name", "campus"}
        ),
    })
@NoArgsConstructor(access = PROTECTED)
public class WebUniversity extends BaseEntity {

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

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "region", nullable = false)
    private UniversityRegion region;

    @NotNull
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Builder
    private WebUniversity(Integer id, String koreanName, Campus campus, UniversityRegion region, String imageUrl) {
        this.id = id;
        this.koreanName = koreanName;
        this.campus = campus;
        this.region = region;
        this.imageUrl = imageUrl;
    }
}
