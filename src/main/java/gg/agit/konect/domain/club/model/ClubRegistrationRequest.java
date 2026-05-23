package gg.agit.konect.domain.club.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "club_registration_request")
@NoArgsConstructor(access = PROTECTED)
public class ClubRegistrationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

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
    @Column(name = "club_emoji", length = 10, nullable = false)
    private String clubEmoji;

    @NotNull
    @Column(name = "short_description", length = 30, nullable = false)
    private String shortDescription;

    @NotNull
    @Column(name = "full_introduction", columnDefinition = "TEXT", nullable = false)
    private String fullIntroduction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "JSON")
    private List<String> imageUrls;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RegistrationStatus status;

    @Builder
    private ClubRegistrationRequest(
        Integer id,
        String universityName,
        String clubName,
        ClubCategory clubCategory,
        String clubTopic,
        String clubEmoji,
        String shortDescription,
        String fullIntroduction,
        List<String> imageUrls,
        RegistrationStatus status
    ) {
        this.id = id;
        this.universityName = universityName;
        this.clubName = clubName;
        this.clubCategory = clubCategory;
        this.clubTopic = clubTopic;
        this.clubEmoji = clubEmoji;
        this.shortDescription = shortDescription;
        this.fullIntroduction = fullIntroduction;
        this.imageUrls = imageUrls;
        this.status = status != null ? status : RegistrationStatus.PENDING;
    }

    public enum RegistrationStatus {
        PENDING, APPROVED, REJECTED
    }
}
