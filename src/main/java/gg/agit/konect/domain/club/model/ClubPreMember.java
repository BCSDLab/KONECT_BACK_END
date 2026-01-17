package gg.agit.konect.domain.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Entity
@Table(
    name = "club_pre_member",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_club_pre_member_club_id_student_number_name",
            columnNames = {"club_id", "student_number", "name"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
// TODO. 초기 회원 처리 완료 후 제거 예정
public class ClubPreMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @NotNull
    @Column(name = "student_number", length = 20, nullable = false)
    private String studentNumber;

    @NotNull
    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Builder
    private ClubPreMember(Integer id, Club club, String studentNumber, String name) {
        this.id = id;
        this.club = club;
        this.studentNumber = studentNumber;
        this.name = name;
    }
}
