package gg.agit.konect.domain.club.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "club_position",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_club_position_club_id_name",
            columnNames = {"club_id", "name"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class ClubPosition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "club_position_group", nullable = false)
    private ClubPositionGroup clubPositionGroup;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Builder
    private ClubPosition(Integer id, String name, ClubPositionGroup clubPositionGroup, Club club) {
        this.id = id;
        this.name = name;
        this.clubPositionGroup = clubPositionGroup;
        this.club = club;
    }

    public boolean isPresident() {
        return clubPositionGroup == ClubPositionGroup.PRESIDENT;
    }

    public boolean isVicePresident() {
        return clubPositionGroup == ClubPositionGroup.VICE_PRESIDENT;
    }

    public boolean isManager() {
        return clubPositionGroup == ClubPositionGroup.MANAGER;
    }

    public boolean isMember() {
        return clubPositionGroup == ClubPositionGroup.MEMBER;
    }

    public boolean canRename() {
        return clubPositionGroup == ClubPositionGroup.MANAGER
            || clubPositionGroup == ClubPositionGroup.MEMBER;
    }

    public boolean canDelete() {
        return clubPositionGroup == ClubPositionGroup.MANAGER
            || clubPositionGroup == ClubPositionGroup.MEMBER;
    }

    public void updateName(String newName) {
        this.name = newName;
    }

    public int getPriority() {
        return clubPositionGroup.getPriority();
    }
}
