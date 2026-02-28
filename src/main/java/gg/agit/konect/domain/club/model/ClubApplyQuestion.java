package gg.agit.konect.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.REQUIRED_CLUB_APPLY_ANSWER_MISSING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.lang.Boolean.TRUE;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import org.springframework.util.StringUtils;

import gg.agit.konect.global.exception.CustomException;
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

    @NotNull
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime deletedAt;

    @Builder
    private ClubApplyQuestion(
        Integer id,
        Club club,
        String question,
        Boolean isRequired,
        Integer displayOrder
    ) {
        this.id = id;
        this.club = club;
        this.question = question;
        this.isRequired = isRequired;
        this.displayOrder = displayOrder;
    }

    public static ClubApplyQuestion of(Club club, String question, Boolean isRequired, Integer displayOrder) {
        return ClubApplyQuestion.builder()
            .club(club)
            .question(question)
            .isRequired(isRequired)
            .displayOrder(displayOrder)
            .build();
    }

    public void validateAnswer(String answer) {
        validateRequiredAnswer(answer);
    }

    public void update(String question, Boolean isRequired) {
        this.question = question;
        this.isRequired = isRequired;
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isSame(String question, Boolean isRequired) {
        return this.question.equals(question) && this.isRequired.equals(isRequired);
    }

    private void validateRequiredAnswer(String answer) {
        if (this.isRequired.equals(TRUE) && !StringUtils.hasText(answer)) {
            throw CustomException.of(REQUIRED_CLUB_APPLY_ANSWER_MISSING);
        }
    }
}
