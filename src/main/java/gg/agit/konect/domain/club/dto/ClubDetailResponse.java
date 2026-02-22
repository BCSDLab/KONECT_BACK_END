package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubDetailResponse(
    @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer id,

    @Schema(description = "동아리 이름", example = "BCSD", requiredMode = REQUIRED)
    String name,

    @Schema(description = "동아리 방 위치", example = "학생회관 101호", requiredMode = REQUIRED)
    String location,

    @Schema(description = "동아리 설명", example = "즐겁게 일하고 열심히 노는 IT 특성화 동아리", requiredMode = REQUIRED)
    String description,

    @Schema(
        description = "동아리 상세 소개",
        example = """
            BCSD에서 얻을 수 있는 경험
            1. IT 실무 경험 및 포트폴리오
            2. 다양한 직군과의 협업 경험
            3. 현업 개발자 및 선배와의 네트워킹
            4. 분야별 취업 멘토링
            """,
        requiredMode = REQUIRED
    )
    String introduce,

    @Schema(description = "동아리 이미지 링크", example = "https://bcsdlab.com/static/img/logo.d89d9cc.png", requiredMode = REQUIRED)
    String imageUrl,

    @Schema(description = "동아리 분과", example = "학술", requiredMode = REQUIRED)
    String categoryName,

    @Schema(description = "동아리 인원 수", example = "30", requiredMode = REQUIRED)
    Integer memberCount,

    @Schema(description = "동아리 모집 정보", requiredMode = REQUIRED)
    InnerRecruitment recruitment,

    @Schema(description = "동아리 회장 ID", example = "1", requiredMode = REQUIRED)
    Integer presidentUserId,

    @Schema(description = "동아리 회장 이름", example = "김철수", requiredMode = REQUIRED)
    String presidentName,

    @Schema(description = "동아리 소속 여부", example = "true", requiredMode = REQUIRED)
    Boolean isMember,

    @Schema(description = "동아리 지원 여부", example = "false", requiredMode = REQUIRED)
    Boolean isApplied
) {
    public record InnerRecruitment(
        @Schema(description = "동아리 모집 상태", example = "ONGOING", requiredMode = REQUIRED)
        RecruitmentStatus status,

        @Schema(description = "동아리 모집 시작일시", example = "2025.11.30 09:00", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
        LocalDateTime startAt,

        @Schema(description = "동아리 모집 마감일시", example = "2025.12.31 18:00", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
        LocalDateTime endAt
    ) {
        public static InnerRecruitment from(ClubRecruitment clubRecruitment, Boolean isRecruitmentEnabled) {
            RecruitmentStatus status = Boolean.TRUE.equals(isRecruitmentEnabled)
                ? RecruitmentStatus.of(clubRecruitment)
                : RecruitmentStatus.CLOSED;

            LocalDateTime startAt = (clubRecruitment != null) ? clubRecruitment.getStartAt() : null;
            LocalDateTime endAt = (clubRecruitment != null) ? clubRecruitment.getEndAt() : null;

            return new InnerRecruitment(status, startAt, endAt);
        }
    }

    public static ClubDetailResponse of(
        Club club,
        Integer memberCount,
        ClubRecruitment clubRecruitment,
        ClubMember president,
        Boolean isMember,
        Boolean isApplied
    ) {
        User presidentUser = president.getUser();

        return new ClubDetailResponse(
            club.getId(),
            club.getName(),
            club.getLocation(),
            club.getDescription(),
            club.getIntroduce(),
            club.getImageUrl(),
            club.getClubCategory().getDescription(),
            memberCount,
            InnerRecruitment.from(clubRecruitment, club.getIsRecruitmentEnabled()),
            presidentUser.getId(),
            presidentUser.getName(),
            isMember,
            isApplied
        );
    }
}
