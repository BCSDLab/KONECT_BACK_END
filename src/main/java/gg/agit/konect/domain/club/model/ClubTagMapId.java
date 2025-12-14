package gg.agit.konect.domain.club.model;

import static lombok.AccessLevel.PROTECTED;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class ClubTagMapId implements Serializable {

    @Column(name = "club_id", nullable = false, updatable = false)
    private Integer clubId;

    @Column(name = "tag_id", nullable = false, updatable = false)
    private Integer tagId;

    public ClubTagMapId(Integer clubId, Integer tagId) {
        this.clubId = clubId;
        this.tagId = tagId;
    }
}
