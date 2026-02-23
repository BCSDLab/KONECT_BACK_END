package gg.agit.konect.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.*;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;
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

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_always_recruiting", columnDefinition = "TINYINT(1)")
    private Boolean isAlwaysRecruiting;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @OneToMany(mappedBy = "clubRecruitment", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private final List<ClubRecruitmentImage> images = new ArrayList<>();

    @Builder
    private ClubRecruitment(
        Integer id,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String content,
        Boolean isAlwaysRecruiting,
        Club club
    ) {
        this.id = id;
        this.startAt = startAt;
        this.endAt = endAt;
        this.content = content;
        this.isAlwaysRecruiting = isAlwaysRecruiting;
        this.club = club;
    }

    public static ClubRecruitment of(
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean isAlwaysRecruiting,
        String content,
        Club club
    ) {
        if (isAlwaysRecruiting) {
            validateAlwaysRecruitingDates(startAt, endAt);
        } else {
            validateRequiredDates(startAt, endAt);
            validateStartAtBeforeEndAt(startAt, endAt);
        }

        return ClubRecruitment.builder()
            .startAt(startAt)
            .endAt(endAt)
            .content(content)
            .club(club)
            .isAlwaysRecruiting(isAlwaysRecruiting)
            .build();
    }

    private static void validateAlwaysRecruitingDates(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null || endAt != null) {
            throw CustomException.of(INVALID_RECRUITMENT_DATE_NOT_ALLOWED);
        }
    }

    private static void validateRequiredDates(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw CustomException.of(INVALID_RECRUITMENT_DATE_REQUIRED);
        }
    }

    private static void validateStartAtBeforeEndAt(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt.isAfter(endAt)) {
            throw CustomException.of(INVALID_RECRUITMENT_PERIOD);
        }
    }

    public void addImage(ClubRecruitmentImage image) {
        this.images.add(image);
    }

    public void update(
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean isAlwaysRecruiting,
        String content
    ) {
        if (isAlwaysRecruiting) {
            validateAlwaysRecruitingDates(startAt, endAt);
        } else {
            validateRequiredDates(startAt, endAt);
            validateStartAtBeforeEndAt(startAt, endAt);
        }

        this.startAt = startAt;
        this.endAt = endAt;
        this.isAlwaysRecruiting = isAlwaysRecruiting;
        this.content = content;
    }
}
