package gg.agit.konect.domain.university.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.university.enums.UniversitySearchKeywordType;
import gg.agit.konect.domain.website.model.WebUniversity;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
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
import lombok.ToString;

@Entity
@Getter
@Table(
    name = "university_search_keyword",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_university_search_keyword_university_keyword",
            columnNames = {"university_id", "normalized_keyword"}
        ),
    })
@NoArgsConstructor(access = PROTECTED)
public class UniversitySearchKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @ToString.Exclude
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private WebUniversity university;

    @NotNull
    @Column(name = "keyword", length = 100, nullable = false)
    private String keyword;

    @NotNull
    @Column(name = "normalized_keyword", length = 100, nullable = false)
    private String normalizedKeyword;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "keyword_type", length = 50, nullable = false)
    private UniversitySearchKeywordType keywordType;

    @Builder
    private UniversitySearchKeyword(
        Integer id,
        WebUniversity university,
        String keyword,
        String normalizedKeyword,
        UniversitySearchKeywordType keywordType
    ) {
        this.id = id;
        this.university = university;
        this.keyword = keyword;
        this.normalizedKeyword = normalizedKeyword;
        this.keywordType = keywordType;
    }
}
