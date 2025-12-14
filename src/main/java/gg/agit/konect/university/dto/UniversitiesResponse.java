package gg.agit.konect.university.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.university.model.University;

import io.swagger.v3.oas.annotations.media.Schema;

public record UniversitiesResponse(
    @Schema(description = "대학 리스트", requiredMode = REQUIRED)
    List<InnerUniversityResponse> universities
) {
    public record InnerUniversityResponse(
        @Schema(description = "대학 고유 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "대학 이름", example = "한국기술교육대학교", requiredMode = REQUIRED)
        String name
    ) {
        public static InnerUniversityResponse from(University university) {
            return new InnerUniversityResponse(
                university.getId(),
                university.getKoreanName()
            );
        }
    }

    public static UniversitiesResponse from(List<University> universities) {
        return new UniversitiesResponse(
            universities.stream()
                .map(InnerUniversityResponse::from)
                .toList()
        );
    }
}
