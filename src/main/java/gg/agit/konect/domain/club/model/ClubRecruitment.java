package gg.agit.konect.domain.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@jakarta.persistence.Table(
    name = "club_recruitment",
    uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(
            name = "uq_club_recruitment_club_id",
            columnNames = {"club_id"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class ClubRecruitment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @Builder
    private ClubRecruitment(Integer id, LocalDate startDate, LocalDate endDate, String content, String imageUrl, Club club) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.content = content;
        this.imageUrl = imageUrl;
        this.club = club;
    }
}
