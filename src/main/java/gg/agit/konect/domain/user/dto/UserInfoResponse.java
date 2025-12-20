package gg.agit.konect.domain.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserInfoResponse(
    @Schema(description = "사용자 이름", example = "이동훈", requiredMode = REQUIRED)
    String name,

    @Schema(description = "사용자 학교 이름", example = "한국기술교육대학교", requiredMode = REQUIRED)
    String universityName,

    @Schema(description = "사용자 학번", example = "2021136091", requiredMode = REQUIRED)
    String studentNumber,

    @Schema(description = "사용자 전화번호", example = "010-1234-5678", requiredMode = NOT_REQUIRED)
    String phoneNumber,

    @Schema(description = "사용자 프로필 이미지 링크", example = "https://stage-static.koreatech.in/konect/User_02.png",requiredMode = REQUIRED)
    String imageUrl,

    @Schema(description = "가입 동아리 개수", example = "1", requiredMode = REQUIRED)
    Integer joinedClubCount,

    @Schema(description = "순공 시간", example = "13:13", requiredMode = REQUIRED)
    @JsonFormat(pattern = "HH:mm")
    LocalTime studyTime,

    @Schema(description = "읽지 않은 총 동아리 연합회 공지", example = "1", requiredMode = REQUIRED)
    Integer unreadCouncilNoticeCount
) {

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
            user.getName(),
            user.getUniversity().getKoreanName(),
            user.getStudentNumber(),
            user.getPhoneNumber(),
            user.getImageUrl()
        );
    }
}
