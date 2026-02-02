package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.club.model.ClubApplyAnswer;
import gg.agit.konect.domain.club.model.ClubApplyQuestion;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubApplicationAnswersResponse(
    @Schema(description = "지원 ID", example = "1", requiredMode = REQUIRED)
    Integer applicationId,

    @Schema(description = "지원자 학번", example = "20250120", requiredMode = REQUIRED)
    String studentNumber,

    @Schema(description = "지원자 이름", example = "이동훈", requiredMode = REQUIRED)
    String name,

    @Schema(description = "지원 일시", example = "2025-01-13T10:30:00", requiredMode = REQUIRED)
    LocalDateTime appliedAt,

    @Schema(description = "지원 답변 목록", requiredMode = REQUIRED)
    List<ClubApplicationAnswerResponse> answers
) {

    public record ClubApplicationAnswerResponse(
        @Schema(description = "문항 ID", example = "1", requiredMode = REQUIRED)
        Integer questionId,

        @Schema(description = "문항 내용", example = "지원 동기를 작성해주세요.", requiredMode = REQUIRED)
        String question,

        @Schema(description = "필수 여부", example = "true", requiredMode = REQUIRED)
        Boolean isRequired,

        @Schema(
            description = "답변 내용(미작성 시 null)",
            example = "동아리 활동을 통해 성장하고 싶습니다.",
            requiredMode = REQUIRED
        )
        String answer
    ) {

        public static ClubApplicationAnswerResponse of(
            ClubApplyQuestion question,
            ClubApplyAnswer applyAnswer
        ) {
            return new ClubApplicationAnswerResponse(
                question.getId(),
                question.getQuestion(),
                question.getIsRequired(),
                applyAnswer == null ? null : applyAnswer.getAnswer()
            );
        }
    }

    public static ClubApplicationAnswersResponse of(
        ClubApply apply,
        List<ClubApplyQuestion> questions,
        List<ClubApplyAnswer> answers
    ) {
        Map<Integer, ClubApplyAnswer> answerMap = answers.stream()
            .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer));

        return new ClubApplicationAnswersResponse(
            apply.getId(),
            apply.getUser().getStudentNumber(),
            apply.getUser().getName(),
            apply.getCreatedAt(),
            questions.stream()
                .map(question -> ClubApplicationAnswerResponse.of(
                    question,
                    answerMap.get(question.getId())
                ))
                .toList()
        );
    }
}
