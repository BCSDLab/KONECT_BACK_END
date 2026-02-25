package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubApplyQuestionsReplaceRequest(
    @NotNull(message = "문항 목록은 필수 입력입니다.")
    @Valid
    @Schema(description = "설문 문항 목록", requiredMode = REQUIRED)
    List<ApplyQuestionRequest> questions
) {
    public record ApplyQuestionRequest(
        @Schema(description = "문항 ID", example = "1", requiredMode = NOT_REQUIRED)
        Integer questionId,

        @NotBlank(message = "문항 내용은 필수 입력입니다.")
        @Size(max = 255, message = "문항 내용은 최대 255자 입니다.")
        @Schema(description = "문항 내용", example = "지원 동기를 입력해주세요.", requiredMode = REQUIRED)
        String question,

        @Schema(description = "필수 여부 (미입력 시 true)", example = "true", requiredMode = NOT_REQUIRED)
        Boolean isRequired
    ) {
        @JsonIgnore
        public boolean isRequiredOrDefault() {
            return isRequired == null || isRequired;
        }
    }
}
