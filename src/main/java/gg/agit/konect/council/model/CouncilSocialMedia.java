package gg.agit.konect.council.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "council_social_media")
@NoArgsConstructor(access = PROTECTED)
public class CouncilSocialMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @NotNull
    @Column(name = "platform_name", length = 50, nullable = false)
    private String platformName;

    @NotNull
    @Column(name = "url", nullable = false)
    private String url;

    @Builder
    private CouncilSocialMedia(Integer id, Council council, String platformName, String url) {
        this.id = id;
        this.council = council;
        this.platformName = platformName;
        this.url = url;
    }
}
