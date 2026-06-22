package gg.agit.konect.domain.club.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.util.ArrayList;
import java.util.List;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Entity
@Table(name = "club_information_update_request")
@NoArgsConstructor(access = PROTECTED)
public class ClubInformationUpdateRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @ToString.Exclude
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "web_club_id", nullable = false)
    private WebClub club;

    @NotNull
    @Column(name = "university_name", nullable = false)
    private String universityName;

    @NotNull
    @Column(name = "club_name", length = 50, nullable = false)
    private String clubName;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "club_category", nullable = false)
    private ClubCategory clubCategory;

    @NotNull
    @Column(name = "club_topic", length = 20, nullable = false)
    private String clubTopic;

    @NotNull
    @Column(name = "short_description", length = 100, nullable = false)
    private String shortDescription;

    @NotNull
    @Column(name = "full_introduction", columnDefinition = "TEXT", nullable = false)
    private String fullIntroduction;

    @OneToMany(mappedBy = "request", cascade = ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ClubInformationUpdateRequestImage> images = new ArrayList<>();

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UpdateRequestStatus status;

    @Builder
    private ClubInformationUpdateRequest(
        Integer id,
        WebClub club,
        String universityName,
        String clubName,
        ClubCategory clubCategory,
        String clubTopic,
        String shortDescription,
        String fullIntroduction,
        UpdateRequestStatus status
    ) {
        this.id = id;
        this.club = club;
        this.universityName = universityName;
        this.clubName = clubName;
        this.clubCategory = clubCategory;
        this.clubTopic = clubTopic;
        this.shortDescription = shortDescription;
        this.fullIntroduction = fullIntroduction;
        this.status = status != null ? status : UpdateRequestStatus.PENDING;
    }

    public void addImages(List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            ClubInformationUpdateRequestImage image = ClubInformationUpdateRequestImage.builder()
                .request(this)
                .imageUrl(imageUrls.get(i))
                .displayOrder(i)
                .build();
            this.images.add(image);
        }
    }

    public enum UpdateRequestStatus {
        PENDING, APPROVED, REJECTED
    }
}
