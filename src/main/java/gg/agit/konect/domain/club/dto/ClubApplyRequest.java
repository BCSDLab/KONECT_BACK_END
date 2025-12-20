package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ClubApplyRequest(
    @Schema(description = "설문 답변 목록", requiredMode = NOT_REQUIRED)
    @Valid
    List<AnswerRequest> answers
) {
    public record AnswerRequest(
        @Schema(description = "설문 질문 ID", example = "1", requiredMode = REQUIRED)
        @NotNull(message = "설문 질문 ID는 필수입니다.")
        Integer questionId,

        @Schema(description = "설문 답변", example = "동아리 활동을 통해 성장하고 싶습니다.", requiredMode = NOT_REQUIRED)
        String answer
    ) {
    }
}
