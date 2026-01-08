package gg.agit.konect.domain.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

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

    @Schema(description = "사용자 이메일", example = "2dh2@naver.com", requiredMode = REQUIRED)
    String email,

    @Schema(description = "사용자 프로필 이미지 링크", example = "https://stage-static.koreatech.in/konect/User_02.png", requiredMode = REQUIRED)
    String imageUrl,

    @Schema(description = "가입 동아리 개수", example = "1", requiredMode = REQUIRED)
    Integer joinedClubCount,

    @Schema(description = "순공 시간(HH:mm)", example = "12:34", requiredMode = REQUIRED)
    String studyTime,

    @Schema(description = "읽지 않은 총 동아리 연합회 공지", example = "1", requiredMode = REQUIRED)
    Long unreadCouncilNoticeCount,

    @Schema(description = "동아리 관리자 여부", example = "true", requiredMode = REQUIRED)
    Boolean isClubManager
) {

    private static final long SECONDS_PER_HOUR = 3600;
    private static final long SECONDS_PER_MINUTE = 60;

    public static UserInfoResponse from(
        User user,
        Integer joinedClubCount,
        Long studyTime,
        Long unreadCouncilNoticeCount,
        Boolean isClubManager
    ) {
        return new UserInfoResponse(
            user.getName(),
            user.getUniversity().getKoreanName(),
            user.getStudentNumber(),
            user.getPhoneNumber(),
            user.getEmail(),
            user.getImageUrl(),
            joinedClubCount,
            formatSecondsToHHmm(studyTime),
            unreadCouncilNoticeCount,
            isClubManager
        );
    }

    private static String formatSecondsToHHmm(Long seconds) {
        long h = seconds / SECONDS_PER_HOUR;
        long m = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;

        return String.format("%02d:%02d", h, m);
    }
}
