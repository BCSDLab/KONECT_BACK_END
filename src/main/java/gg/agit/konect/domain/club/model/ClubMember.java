package gg.agit.konect.domain.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
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

    @Column(name = "is_fee_paid")
    private Boolean isFeePaid;

    @Builder
    private ClubMember(Club club, User user, ClubPosition clubPosition, Boolean isFeePaid) {
        this.id = new ClubMemberId(club.getId(), user.getId());
        this.club = club;
        this.user = user;
        this.clubPosition = clubPosition;
        this.isFeePaid = isFeePaid;
    }

    public boolean isPresident() {
        return clubPosition.isPresident();
    }

    public boolean isSameUser(Integer userId) {
        return this.user.getId().equals(userId);
    }

    public void changePosition(ClubPosition clubPosition) {
        this.clubPosition = clubPosition;
    }

    public ClubPositionGroup getPositionGroup() {
        return this.clubPosition.getClubPositionGroup();
    }

    public boolean canManage(ClubMember target) {
        return this.getPositionGroup().canManage(target.getPositionGroup());
    }
}
