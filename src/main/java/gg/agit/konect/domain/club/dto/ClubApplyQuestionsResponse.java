package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubApplyQuestionsResponse(
    @Schema(description = "설문 문항 목록", requiredMode = REQUIRED)
    List<ApplyQuestion> questions
) {
    public record ApplyQuestion(
        @Schema(description = "문항 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "문항 내용", example = "지원 동기를 입력해주세요.", requiredMode = REQUIRED)
        String question,

        @Schema(description = "필수 여부", example = "true", requiredMode = REQUIRED)
        Boolean isRequired
    ) {
        public static ApplyQuestion from(ClubApplyQuestion question) {
            return new ApplyQuestion(
                question.getId(),
                question.getQuestion(),
                question.getIsRequired()
            );
        }
    }

    public static ClubApplyQuestionsResponse from(List<ClubApplyQuestion> questions) {
        return new ClubApplyQuestionsResponse(
            questions.stream()
                .map(ApplyQuestion::from)
                .toList()
        );
    }
}
