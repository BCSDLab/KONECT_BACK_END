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
@Table(name = "club_position_fee")
@NoArgsConstructor(access = PROTECTED)
public class ClubPositionFee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "fee", nullable = false)
    private Integer fee;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_position_id", nullable = false)
    private ClubPosition clubPosition;

    @Builder
    private ClubPositionFee(Integer id, Integer fee, ClubPosition clubPosition) {
        this.id = id;
        this.fee = fee;
        this.clubPosition = clubPosition;
    }
}
