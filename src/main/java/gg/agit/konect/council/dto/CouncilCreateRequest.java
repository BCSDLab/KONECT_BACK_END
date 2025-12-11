package gg.agit.konect.council.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.model.CouncilSocialMedia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CouncilCreateRequest(
    @NotEmpty(message = "총동아리연합회 이름은 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 이름은 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 이름", example = "개화", requiredMode = REQUIRED)
    String name,

    @NotEmpty(message = "총동아리연합회 설명은 필수 입력입니다.")
    @Schema(description = "총동아리연합회 설명", example = "총동아리연합회는 ...", requiredMode = REQUIRED)
    String introduce,

    @NotEmpty(message = "총동아리연합회 위치는 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 위치는 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 위치", example = "학생회관 2층 202호", requiredMode = REQUIRED)
    String location,

    @NotEmpty(message = "총동아리연합회 퍼스널 컬러는 필수 입력깁니다.")
    @Size(max = 255, message = "총동아리연합회 퍼스널 컬러는 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 퍼스널 컬러", example = "#FF5733", requiredMode = REQUIRED)
    String personalColor,

    @NotEmpty(message = "총동아리연합회 운영 시간을 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 운영 시간은 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 운영 시간", example = "평일 09:00 ~ 18:00", requiredMode = REQUIRED)
    String operatingHour,

    @Schema(description = "총동아리연합회 인스타 주소", example = "https://www.instagram.com/koreatech_council", requiredMode = REQUIRED)
    String instagramUrl
) {
    public record InnerSocialMedia(
        @NotBlank(message = "플랫폼 이름은 필수 입력값입니다.")
        @Size(max = 255, message = "플랫폼 이름은 최대 255자 입니다.")
        @Schema(description = "플랫폼 이름", example = "인스타그램", requiredMode = REQUIRED)
        String name,

        @NotBlank(message = "플랫폼 url은 필수 입력값입니다.")
        @Size(max = 255, message = "플랫폼 url은 최대 255자 입니다.")
        @Schema(description = "플랫폼 url", example = "https://www.instagram.com/koreatech_council", requiredMode = REQUIRED)
        String url
    ) {
        public CouncilSocialMedia toEntity(Council council) {
            return CouncilSocialMedia.builder()
                .council(council)
                .platformName(name)
                .url(url)
                .build();
        }
    }

    public Council toEntity() {
        return Council.builder()
            .name(name)
            .introduce(introduce)
            .location(location)
            .personalColor(personalColor)
            .build();
    }
}
