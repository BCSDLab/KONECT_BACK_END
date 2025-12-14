package gg.agit.konect.domain.club.model;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class ClubMemberId {

    @Column(name = "club_id", nullable = false, updatable = false)
    private Integer clubId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Integer userId;

    public ClubMemberId(Integer clubId, Integer userId) {
        this.clubId = clubId;
        this.userId = userId;
    }
}
