package gg.agit.konect.domain.club.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "club_tag",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_club_tag_name",
            columnNames = {"name"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class ClubTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    @Builder
    private ClubTag(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
