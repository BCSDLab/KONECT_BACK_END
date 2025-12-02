package gg.agit.konect.club.model;

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

@Entity
@Getter
@Table(name = "club_position")
@NoArgsConstructor(access = PROTECTED)
public class ClubPosition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_position_group_id", nullable = false)
    private ClubPositionGroup clubPositionGroup;

    @Builder
    private ClubPosition(Integer id, String name, Club club, ClubPositionGroup clubPositionGroup) {
        this.id = id;
        this.name = name;
        this.club = club;
        this.clubPositionGroup = clubPositionGroup;
    }
}
