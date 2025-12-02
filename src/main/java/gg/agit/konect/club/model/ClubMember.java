package gg.agit.konect.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static java.lang.Boolean.FALSE;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.common.model.BaseEntity;
import gg.agit.konect.user.model.User;
import jakarta.persistence.Column;
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
@Table(name = "club_member")
@NoArgsConstructor(access = PROTECTED)
public class ClubMember extends BaseEntity {

    @EmbeddedId
    private ClubMemberId id;

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin = FALSE;

    @MapsId(value = "clubId")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @MapsId(value = "userId")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_position_id", nullable = false)
    private ClubPosition clubPosition;

    @Builder
    private ClubMember(Boolean isAdmin, Club club, User user, ClubPosition clubPosition) {
        this.id = new ClubMemberId(club.getId(), user.getId());
        this.isAdmin = isAdmin;
        this.club = club;
        this.user = user;
        this.clubPosition = clubPosition;
    }
}
