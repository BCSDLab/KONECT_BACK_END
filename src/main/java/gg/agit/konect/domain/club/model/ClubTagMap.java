package gg.agit.konect.domain.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.common.model.BaseEntity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "club_tag_map")
@NoArgsConstructor(access = PROTECTED)
public class ClubTagMap extends BaseEntity {

    @EmbeddedId
    private ClubTagMapId id;

    @MapsId(value = "clubId")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @MapsId(value = "tagId")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "tag_id", nullable = false, updatable = false)
    private ClubTag tag;

    @Builder
    private ClubTagMap(Club club, ClubTag tag) {
        this.id = new ClubTagMapId(club.getId(), tag.getId());
        this.club = club;
        this.tag = tag;
    }
}
