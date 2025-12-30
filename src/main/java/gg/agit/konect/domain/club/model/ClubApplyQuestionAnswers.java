package gg.agit.konect.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_APPLY_QUESTION;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import gg.agit.konect.global.exception.CustomException;

public record ClubApplyQuestionAnswers(
    List<ClubApplyQuestion> questions,
    Map<Integer, String> answers
) {
    public static ClubApplyQuestionAnswers of(List<ClubApplyQuestion> questions, Map<Integer, String> answers) {
        validate(questions, answers);
        return new ClubApplyQuestionAnswers(questions, answers);
    }

    private static void validate(List<ClubApplyQuestion> questions, Map<Integer, String> answers) {
        Map<Integer, ClubApplyQuestion> questionMap = questions.stream()
            .collect(Collectors.toMap(ClubApplyQuestion::getId, question -> question));
        Set<Integer> answeredQuestionIds = new HashSet<>();

        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            Integer questionId = entry.getKey();
            String answer = entry.getValue();

            if (!questionMap.containsKey(questionId)) {
                throw CustomException.of(NOT_FOUND_CLUB_APPLY_QUESTION);
            }

            ClubApplyQuestion question = questionMap.get(questionId);
            question.validateAnswer(answer);

            if (StringUtils.hasText(answer)) {
                answeredQuestionIds.add(questionId);
            }
        }

        for (ClubApplyQuestion question : questions) {
            if (!answeredQuestionIds.contains(question.getId())) {
                question.validateAnswer(null);
            }
        }
    }

    public List<ClubApplyAnswer> toEntities(ClubApply apply) {
        Map<Integer, ClubApplyQuestion> questionMap = questions.stream()
            .collect(Collectors.toMap(ClubApplyQuestion::getId, question -> question));

        return answers.entrySet().stream()
            .filter(entry -> StringUtils.hasText(entry.getValue()))
            .map(entry -> ClubApplyAnswer.of(apply, questionMap.get(entry.getKey()), entry.getValue()))
            .toList();
    }
}
