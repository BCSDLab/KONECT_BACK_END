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
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "club_apply_question")
@NoArgsConstructor(access = PROTECTED)
public class ClubApplyQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @NotNull
    @Column(name = "question", length = 255, nullable = false)
    private String question;

    @NotNull
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    @Builder
    private ClubApplyQuestion(Integer id, Club club, String question, Boolean isRequired) {
        this.id = id;
        this.club = club;
        this.question = question;
        this.isRequired = isRequired;
    }
}
