package gg.agit.konect.council.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.council.model.Council;
import gg.agit.konect.council.model.CouncilOperatingHour;
import gg.agit.konect.council.model.CouncilSocialMedia;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CouncilUpdateRequest(
    @NotEmpty(message = "총동아리연합회 이름은 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 이름은 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 이름", example = "개화", requiredMode = REQUIRED)
    String name,

    @NotEmpty(message = "총동아리연합회 설명은 필수 입력입니다.")
    @Schema(description = "총동아리연합회 설명", example = "홍총아리연합회는 ...", requiredMode = REQUIRED)
    String introduce,

    @NotEmpty(message = "총동아리연합회 위치는 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 위치는 최대 255자 입니다.")
    @Schema(description = "총동아리연합회 위치", example = "학생회관 2층 202호", requiredMode = REQUIRED)
    String location,

    @NotEmpty(message = "총동아리연합회 전화번호는 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 전화번호는 최대 255자 입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    @Schema(description = "총동아리연합회 전화번호", example = "041-560-1234", requiredMode = REQUIRED)
    String phoneNumber,

    @Schema(description = "총동아리연합회 운영 시간 리스트", requiredMode = REQUIRED)
    List<InnerOperatingHour> operatingHours,

    @NotEmpty(message = "총동아리연합회 이메일은 필수 입력입니다.")
    @Size(max = 255, message = "총동아리연합회 이메일은 최대 255자 입니다.")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "총동아리연합회 이메일", example = "council@koreatech.ac.kr", requiredMode = REQUIRED)
    String email,

    @Schema(description = "총동아리연합회 소셜미디어", requiredMode = REQUIRED)
    List<InnerSocialMedia> socialMedias
) {
    public record InnerOperatingHour(
        @NotNull(message = "운영 요일은 필수 입력값입니다.")
        @Schema(description = "요일", example = "MONDAY", requiredMode = REQUIRED)
        DayOfWeek dayOfWeek,

        @Schema(description = "시작 시간", example = "09:00", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "HH:mm")
        LocalTime openTime,

        @Schema(description = "마감 시간", example = "18:00", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "HH:mm")
        LocalTime closeTime,

        @NotNull(message = "마감 여부는 필수 입력값입니다.")
        @Schema(description = "마감 여부", example = "false", requiredMode = REQUIRED)
        Boolean isClosed
    ) {
        public CouncilOperatingHour toEntity(Council council) {
            return CouncilOperatingHour.builder()
                .council(council)
                .dayOfWeek(dayOfWeek)
                .openTime(openTime)
                .closeTime(closeTime)
                .isClosed(isClosed)
                .build();
        }
    }

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
}
