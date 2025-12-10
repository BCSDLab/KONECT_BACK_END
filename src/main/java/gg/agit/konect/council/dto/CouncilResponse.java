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

    @Schema(description = "총동아리연합회 운영 시간 리스트", requiredMode = REQUIRED)
    List<InnerOperatingHour> operatingHours,

    @Schema(description = "총동아리연합회 전화번호", example = "041-560-1234", requiredMode = REQUIRED)
    String phoneNumber,

    @Schema(description = "총동아리연합회 이메일", example = "council@koreatech.ac.kr", requiredMode = REQUIRED)
    String email,

    @Schema(description = "총동아리연합회 인스타 주소", example = "https://www.instagram.com/koreatech_council", requiredMode = REQUIRED)
    String instagramUrl
) {
    public record InnerOperatingHour(
        @Schema(description = "요일", example = "MONDAY", requiredMode = REQUIRED)
        DayOfWeek dayOfWeek,

        @Schema(description = "시작 시간", example = "09:00", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "HH:mm")
        LocalTime openTime,

        @Schema(description = "마감 시간", example = "18:00", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "HH:mm")
        LocalTime closeTime,

        @Schema(description = "마감 여부", example = "false", requiredMode = REQUIRED)
        Boolean isClosed
    ) {
        public static InnerOperatingHour from(CouncilOperatingHour operatingHour) {
            return new InnerOperatingHour(
                operatingHour.getDayOfWeek(),
                operatingHour.getOpenTime(),
                operatingHour.getCloseTime(),
                operatingHour.getIsClosed()
            );
        }
    }

    public static CouncilResponse of(
        Council council,
        List<CouncilOperatingHour> operatingHours
    ) {
        return new CouncilResponse(
            council.getId(),
            council.getName(),
            council.getIntroduce(),
            council.getLocation(),
            council.getPersonalColor(),
            council.getOperatingHour(),
            operatingHours.stream()
                .map(InnerOperatingHour::from)
                .toList(),
            council.getPhoneNumber(),
            council.getEmail(),
            council.getInstagramUrl()
        );
    }
}
