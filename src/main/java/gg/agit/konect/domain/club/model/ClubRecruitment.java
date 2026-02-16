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

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @OneToMany(mappedBy = "clubRecruitment", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private final List<ClubRecruitmentImage> images = new ArrayList<>();

    /**
     * Constructs a ClubRecruitment instance with the given properties.
     *
     * @param id                 the entity identifier (may be null for new instances)
     * @param startDate          the recruitment start date; may be null when `isAlwaysRecruiting` is true
     * @param endDate            the recruitment end date; may be null when `isAlwaysRecruiting` is true
     * @param content            the recruitment description content
     * @param isAlwaysRecruiting whether the club is recruiting indefinitely
     * @param club               the associated Club entity
     */
    @Builder
    private ClubRecruitment(
        Integer id,
        LocalDate startDate,
        LocalDate endDate,
        String content,
        Boolean isAlwaysRecruiting,
        Club club
    ) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.content = content;
        this.isAlwaysRecruiting = isAlwaysRecruiting;
        this.club = club;
    }

    /**
     * Create a ClubRecruitment with the given dates, recruitment mode, content, and associated club.
     *
     * Validates dates according to the recruitment mode: when `isAlwaysRecruiting` is `true`, both
     * `startDate` and `endDate` must be null; otherwise both dates must be non-null and
     * `startDate` must be on or before `endDate`.
     *
     * @param startDate the recruitment start date, or null if recruitment is perpetual
     * @param endDate the recruitment end date, or null if recruitment is perpetual
     * @param isAlwaysRecruiting whether the recruitment is perpetual (no start/end dates)
     * @param content the recruitment content text
     * @param club the club associated with this recruitment
     * @return a new ClubRecruitment initialized with the provided values
     */
    public static ClubRecruitment of(
        LocalDate startDate,
        LocalDate endDate,
        Boolean isAlwaysRecruiting,
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

    /**
     * Updates the recruitment period, perpetual-recruitment flag, and content for this recruitment.
     *
     * @param startDate           the recruitment start date; may be null when `isAlwaysRecruiting` is true
     * @param endDate             the recruitment end date; may be null when `isAlwaysRecruiting` is true
     * @param isAlwaysRecruiting  if true, recruitment is perpetual and both dates must be null; if false, both dates are required and startDate must be on or before endDate
     * @param content             the recruitment content/description
     */
    public void update(
        LocalDate startDate,
        LocalDate endDate,
        Boolean isAlwaysRecruiting,
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
        this.content = content;
    }
}