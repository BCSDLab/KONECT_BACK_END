package gg.agit.konect.domain.website.model;

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
@Table(name = "web_club")
@NoArgsConstructor(access = PROTECTED)
public class WebClub extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "club_category", nullable = false)
    private ClubCategory clubCategory;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private WebUniversity university;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "topic", length = 20, nullable = false)
    private String topic;

    @Column(name = "description", length = 30, nullable = false)
    private String description;

    @Column(name = "introduce", columnDefinition = "TEXT", nullable = false)
    private String introduce;

    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    @Builder
    private WebClub(
        Integer id,
        ClubCategory clubCategory,
        WebUniversity university,
        String name,
        String topic,
        String description,
        String introduce,
        String imageUrl
    ) {
        this.id = id;
        this.clubCategory = clubCategory;
        this.university = university;
        this.name = name;
        this.topic = topic;
        this.description = description;
        this.introduce = introduce;
        this.imageUrl = imageUrl;
    }
}
