package gg.agit.konect.domain.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "club_recruitment_image")
@NoArgsConstructor(access = PROTECTED)
public class ClubRecruitmentImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @NotNull
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_recruitment_id", nullable = false)
    private ClubRecruitment clubRecruitment;

    @Builder
    private ClubRecruitmentImage(Integer id, String url, Integer displayOrder, ClubRecruitment clubRecruitment) {
        this.id = id;
        this.url = url;
        this.displayOrder = displayOrder;
        this.clubRecruitment = clubRecruitment;
    }
}
