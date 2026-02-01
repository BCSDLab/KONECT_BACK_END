package gg.agit.konect.domain.appversion.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.appversion.enums.PlatformType;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "app_version")
@NoArgsConstructor(access = PROTECTED)
public class AppVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20, nullable = false)
    private PlatformType platform;

    @Column(name = "version", length = 20, nullable = false)
    private String version;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @Builder
    private AppVersion(
        PlatformType platform,
        String version,
        String releaseNotes
    ) {
        this.platform = platform;
        this.version = version;
        this.releaseNotes = releaseNotes;
    }
}
