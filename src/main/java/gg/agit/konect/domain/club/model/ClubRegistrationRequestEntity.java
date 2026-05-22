package gg.agit.konect.domain.club.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.util.ArrayList;
import java.util.List;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "club_registration_request")
@NoArgsConstructor(access = PROTECTED)
public class ClubRegistrationRequestEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "university_name", length = 50, nullable = false)
    private String universityName;

    @Column(name = "club_name", length = 50, nullable = false)
    private String clubName;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "club_category", length = 20, nullable = false)
    private ClubCategory clubCategory;

    @Column(name = "topic", length = 20, nullable = false)
    private String topic;

    @Column(name = "emoji", length = 20, nullable = false)
    private String emoji;

    @Column(name = "description", length = 30, nullable = false)
    private String description;

    @Column(name = "introduce", columnDefinition = "TEXT", nullable = false)
    private String introduce;

    @OneToMany(mappedBy = "clubRegistrationRequest", cascade = ALL, orphanRemoval = true)
    private List<ClubRegistrationRequestMedia> media = new ArrayList<>();

    @Builder
    private ClubRegistrationRequestEntity(
        Integer id,
        String universityName,
        String clubName,
        ClubCategory clubCategory,
        String topic,
        String emoji,
        String description,
        String introduce
    ) {
        this.id = id;
        this.universityName = universityName;
        this.clubName = clubName;
        this.clubCategory = clubCategory;
        this.topic = topic;
        this.emoji = emoji;
        this.description = description;
        this.introduce = introduce;
    }

    public static ClubRegistrationRequestEntity from(ClubRegistrationRequest request) {
        ClubRegistrationRequestEntity entity = ClubRegistrationRequestEntity.builder()
            .universityName(request.universityName())
            .clubName(request.clubName())
            .clubCategory(request.clubCategory())
            .topic(request.topic())
            .emoji(request.emoji())
            .description(request.description())
            .introduce(request.introduce())
            .build();

        for (int index = 0; index < request.mediaUrls().size(); index++) {
            entity.addMedia(request.mediaUrls().get(index), index);
        }
        return entity;
    }

    public List<String> getMediaUrls() {
        return media.stream()
            .sorted(java.util.Comparator.comparing(ClubRegistrationRequestMedia::getDisplayOrder))
            .map(ClubRegistrationRequestMedia::getUrl)
            .toList();
    }

    private void addMedia(String url, Integer displayOrder) {
        media.add(ClubRegistrationRequestMedia.of(url, displayOrder, this));
    }
}
