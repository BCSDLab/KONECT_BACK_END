package gg.agit.konect.domain.club.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "club_information_update_request")
@NoArgsConstructor(access = PROTECTED)
public class ClubInformationUpdateRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @NotNull
    @Column(name = "club_name", length = 50, nullable = false)
    private String clubName;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "club_category", nullable = false)
    private ClubCategory clubCategory;

    @NotNull
    @Column(name = "short_description", length = 25, nullable = false)
    private String shortDescription;

    @NotNull
    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    @NotNull
    @Column(name = "location", length = 255, nullable = false)
    private String location;

    @NotNull
    @Column(name = "full_introduction", columnDefinition = "TEXT", nullable = false)
    private String fullIntroduction;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UpdateRequestStatus status;

    @Builder
    private ClubInformationUpdateRequest(
        Integer id,
        Club club,
        String clubName,
        ClubCategory clubCategory,
        String shortDescription,
        String imageUrl,
        String location,
        String fullIntroduction,
        UpdateRequestStatus status
    ) {
        this.id = id;
        this.club = club;
        this.clubName = clubName;
        this.clubCategory = clubCategory;
        this.shortDescription = shortDescription;
        this.imageUrl = imageUrl;
        this.location = location;
        this.fullIntroduction = fullIntroduction;
        this.status = status != null ? status : UpdateRequestStatus.PENDING;
    }

    public enum UpdateRequestStatus {
        PENDING, APPROVED, REJECTED
    }
}
