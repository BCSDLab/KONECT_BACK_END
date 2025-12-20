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
@Table(name = "club_apply_answer")
@NoArgsConstructor(access = PROTECTED)
public class ClubApplyAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "apply_id", nullable = false, updatable = false)
    private ClubApply apply;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "question_id", nullable = false, updatable = false)
    private ClubApplyQuestion question;

    @NotNull
    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Builder
    private ClubApplyAnswer(
        Integer id,
        ClubApply apply,
        ClubApplyQuestion question,
        String answer
    ) {
        this.id = id;
        this.apply = apply;
        this.question = question;
        this.answer = answer;
    }
}
