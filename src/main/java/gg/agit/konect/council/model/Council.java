package gg.agit.konect.council.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.common.model.BaseEntity;
import gg.agit.konect.university.model.University;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "council")
@NoArgsConstructor(access = PROTECTED)
public class Council extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @NotNull
    @Column(name = "introduce", columnDefinition = "TEXT", nullable = false)
    private String introduce;

    @NotNull
    @Column(name = "location", nullable = false)
    private String location;

    @NotNull
    @Column(name = "personal_color", nullable = false)
    private String personalColor;

    @NotNull
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @NotNull
    @Column(name = "instagram_url", nullable = false)
    private String instagramUrl;

    @NotNull
    @Column(name = "operating_hour", nullable = false)
    private String operatingHour;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Builder
    private Council(
        Integer id,
        String name,
        String imageUrl,
        String introduce,
        String location,
        String personalColor,
        String phoneNumber,
        String email,
        String instagramUrl,
        String operatingHour,
        University university
    ) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.introduce = introduce;
        this.location = location;
        this.personalColor = personalColor;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.instagramUrl = instagramUrl;
        this.operatingHour = operatingHour;
        this.university = university;
    }

    public void update(
        String name,
        String imageUrl,
        String introduce,
        String location,
        String personalColor,
        String instagramUrl,
        String operatingHour
    ) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.introduce = introduce;
        this.location = location;
        this.personalColor = personalColor;
        this.instagramUrl = instagramUrl;
        this.operatingHour = operatingHour;
    }
}
