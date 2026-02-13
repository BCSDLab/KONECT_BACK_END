package gg.agit.konect.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.*;
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

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_always_recruiting", columnDefinition = "TINYINT(1)")
    private Boolean isAlwaysRecruiting;

    @Column(name = "is_fee_required", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isFeeRequired;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @OneToMany(mappedBy = "clubRecruitment", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private final List<ClubRecruitmentImage> images = new ArrayList<>();

    @Builder
    private ClubRecruitment(
        Integer id,
        LocalDate startDate,
        LocalDate endDate,
        String content,
        Boolean isAlwaysRecruiting,
        Boolean isFeeRequired,
        Club club
    ) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.content = content;
        this.isAlwaysRecruiting = isAlwaysRecruiting;
        this.isFeeRequired = isFeeRequired;
        this.club = club;
    }

    public static ClubRecruitment of(
        LocalDate startDate,
        LocalDate endDate,
        Boolean isAlwaysRecruiting,
        Boolean isFeeRequired,
        String content,
        Club club
    ) {
        if (isAlwaysRecruiting) {
            validateAlwaysRecruitingDates(startDate, endDate);
        } else {
            validateRequiredDates(startDate, endDate);
            validateStartDateBeforeEndDate(startDate, endDate);
        }

        return ClubRecruitment.builder()
            .startDate(startDate)
            .endDate(endDate)
            .content(content)
            .club(club)
            .isAlwaysRecruiting(isAlwaysRecruiting)
            .isFeeRequired(isFeeRequired)
            .build();
    }

    private static void validateAlwaysRecruitingDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null || endDate != null) {
            throw CustomException.of(INVALID_RECRUITMENT_DATE_NOT_ALLOWED);
        }
    }

    private static void validateRequiredDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw CustomException.of(INVALID_RECRUITMENT_DATE_REQUIRED);
        }
    }

    private static void validateStartDateBeforeEndDate(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw CustomException.of(INVALID_RECRUITMENT_PERIOD);
        }
    }

    public void addImage(ClubRecruitmentImage image) {
        this.images.add(image);
    }

    public void update(
        LocalDate startDate,
        LocalDate endDate,
        Boolean isAlwaysRecruiting,
        Boolean isFeeRequired,
        String content
    ) {
        if (isAlwaysRecruiting) {
            validateAlwaysRecruitingDates(startDate, endDate);
        } else {
            validateRequiredDates(startDate, endDate);
            validateStartDateBeforeEndDate(startDate, endDate);
        }

        this.startDate = startDate;
        this.endDate = endDate;
        this.isAlwaysRecruiting = isAlwaysRecruiting;
        this.isFeeRequired = isFeeRequired;
        this.content = content;
    }
}
