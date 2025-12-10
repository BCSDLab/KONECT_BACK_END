package gg.agit.konect.council.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.council.model.Council;
import io.swagger.v3.oas.annotations.media.Schema;

public record CouncilResponse(
    @Schema(description = "총동아리연합회 고유 id", example = "1", requiredMode = REQUIRED)
    Integer id,

    @Schema(description = "총동아리연합회 이름", example = "개화", requiredMode = REQUIRED)
    String name,

    @Schema(description = "총동아리연합회 설명", example = "홍총아리연합회는 ...", requiredMode = REQUIRED)
    String introduce,

    @Schema(description = "총동아리연합회 위치", example = "학생회관 2층 202호", requiredMode = REQUIRED)
    String location,

    @Schema(description = "총동아리연합회 퍼스널 컬러", example = "#FF5733", requiredMode = REQUIRED)
    String personalColor,

    @Schema(description = "총동아리연합회 운영 시간", example = "평일 09:00 ~ 18:00", requiredMode = REQUIRED)
    String operatingHour,

    @Schema(description = "총동아리연합회 인스타 주소", example = "https://www.instagram.com/koreatech_council", requiredMode = REQUIRED)
    String instagramUrl
) {
    public static CouncilResponse from(Council council) {
        return new CouncilResponse(
            council.getId(),
            council.getName(),
            council.getIntroduce(),
            council.getLocation(),
            council.getPersonalColor(),
            council.getOperatingHour(),
            council.getInstagramUrl()
        );
    }
}
