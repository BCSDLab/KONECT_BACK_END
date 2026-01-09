package gg.agit.konect.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.INVALID_RECRUITMENT_DATE_ORDER;
import static gg.agit.konect.global.code.ApiResponseCode.INVALID_RECRUITMENT_DATE_REQUIRED;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "club_recruitment",
    uniqueConstraints = {
        @UniqueConstraint(
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

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @OneToMany(mappedBy = "clubRecruitment", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private List<ClubRecruitmentImage> images = new ArrayList<>();

    @Builder
    private ClubRecruitment(
        Integer id,
        LocalDate startDate,
        LocalDate endDate,
        String content,
        Club club
    ) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.content = content;
        this.club = club;
    }

    public static ClubRecruitment of(
        LocalDate startDate,
        LocalDate endDate,
        Boolean isAlwaysRecruiting,
        String content,
        Club club
    ) {
        LocalDate finalStartDate = startDate;
        LocalDate finalEndDate = endDate;

        if (isAlwaysRecruiting) {
            finalStartDate = LocalDate.of(1900, 1, 1);
            finalEndDate = LocalDate.of(2999, 12, 31);
        } else {
            if (startDate == null || endDate == null) {
                throw CustomException.of(INVALID_RECRUITMENT_DATE_REQUIRED);
            }

            if (startDate.isAfter(endDate)) {
                throw CustomException.of(INVALID_RECRUITMENT_DATE_ORDER);
            }
        }

        return ClubRecruitment.builder()
            .startDate(finalStartDate)
            .endDate(finalEndDate)
            .content(content)
            .club(club)
            .build();
    }

    public void addImage(ClubRecruitmentImage image) {
        this.images.add(image);
    }
}
