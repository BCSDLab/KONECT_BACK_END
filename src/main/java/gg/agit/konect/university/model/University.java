package gg.agit.konect.university.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "university")
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
    @Column(name = "english_name", nullable = false)
    private String englishName;

    @NotNull
    @Column(name = "email_domain", nullable = false)
    private String emailDomain;

    @Builder
    private University(Integer id, String koreanName, String englishName, String emailDomain) {
        this.id = id;
        this.koreanName = koreanName;
        this.englishName = englishName;
        this.emailDomain = emailDomain;
    }
}
