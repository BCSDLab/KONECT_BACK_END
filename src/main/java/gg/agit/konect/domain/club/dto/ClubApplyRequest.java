package gg.agit.konect.domain.club.dto;

import static gg.agit.konect.global.code.ApiResponseCode.DUPLICATE_CLUB_APPLY_QUESTION;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gg.agit.konect.global.exception.CustomException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ClubApplyRequest(
    @Schema(description = "설문 답변 목록", requiredMode = REQUIRED)
    @NotNull(message = "설문 답변 목록은 필수입니다.")
    @Valid
    List<InnerClubQuestionAnswer> answers
) {
    public record InnerClubQuestionAnswer(
        @Schema(description = "설문 질문 ID", example = "1", requiredMode = REQUIRED)
        @NotNull(message = "설문 질문 ID는 필수입니다.")
        Integer questionId,

        @Schema(description = "설문 답변", example = "동아리 활동을 통해 성장하고 싶습니다.", requiredMode = NOT_REQUIRED)
        String answer
    ) {

    }

    public Map<Integer, String> toAnswerMap() {
        Map<Integer, String> answerMap = new HashMap<>();

        for (InnerClubQuestionAnswer answer : answers) {
            if (answerMap.containsKey(answer.questionId())) {
                throw CustomException.of(DUPLICATE_CLUB_APPLY_QUESTION);
            }
            answerMap.put(answer.questionId(), answer.answer());
        }

        return answerMap;
    }
}
