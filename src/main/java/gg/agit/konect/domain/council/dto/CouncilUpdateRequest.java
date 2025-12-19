package gg.agit.konect.domain.council.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CouncilUpdateRequest(
    @NotEmpty(message = "총동아리연합회 이름은 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 이름은 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 이름", example = "개화", requiredMode = REQUIRED)
    String name,

    @NotEmpty(message = "총동아리연합회 이미지 링크는 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 이미지 링크는 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 이미지 링크", example = "https://konect.kro.kr/image.jpg", requiredMode = REQUIRED)
    String imageUrl,

    @NotEmpty(message = "총동아리연합회 설명은 필수 입력입니다.")
    @Schema(description = "총동아리연합회 설명", example = "총동아리연합회는 ...", requiredMode = REQUIRED)
    String introduce,

    @NotEmpty(message = "총동아리연합회 위치는 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 위치는 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 위치", example = "학생회관 2층 202호", requiredMode = REQUIRED)
    String location,

    @NotEmpty(message = "총동아리연합회 퍼스널 컬러는 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 퍼스널 컬러는 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 퍼스널 컬러", example = "#FF5733", requiredMode = REQUIRED)
    String personalColor,

    @NotEmpty(message = "총동아리연합회 전화번호는 필수 입력입니다.")
    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    @Schema(description = "총동아리연합회 전화번호", example = "010-1234-5678", requiredMode = REQUIRED)
    String phoneNumber,

    @NotEmpty(message = "총동아리연합회 이메일은 필수 입력입니다.")
    @Pattern(regexp="^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])+[.][a-zA-Z]{2,3}$", message="이메일 주소 양식을 확인해주세요")
    @Size(max = 255, message = "총동아리연합회 이메일은 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 이메일", example = "council@example.com", requiredMode = REQUIRED)
    String email,

    @NotEmpty(message = "총동아리연합회 운영 시간은 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 운영 시간은 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 운영 시간", example = "평일 09:00 ~ 18:00", requiredMode = REQUIRED)
    String operatingHour,

    @NotEmpty(message = "총동아리연합회 인스타 주소는 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 인스타 주소는 최대 255자 입니다.")
    @Pattern(regexp = "^https?://(www\\.)?instagram\\.com/[a-zA-Z0-9._]+/?$", message = "올바른 인스타그램 URL 형식이 아닙니다.")
    @Schema(description = "총동아리연합회 인스타 주소", example = "https://www.instagram.com/koreatech_council", requiredMode = REQUIRED)
    String instagramUrl
) {

}
