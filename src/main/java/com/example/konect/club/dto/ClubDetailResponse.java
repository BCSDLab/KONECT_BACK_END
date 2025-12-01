package com.example.konect.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;

import com.example.konect.club.enums.RecruitmentStatus;
import com.example.konect.club.model.Club;
import com.example.konect.club.model.ClubExecutive;
import com.example.konect.club.model.ClubRecruitment;
import com.example.konect.user.model.User;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubDetailResponse(
    @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer id,

    @Schema(description = "동아리 이름", example = "BCSD", requiredMode = REQUIRED)
    String name,

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
    Long memberCount,

    @Schema(description = "동아리 모집 정보", requiredMode = REQUIRED)
    InnerRecruitment recruitment,

    @Schema(description = "동아리 대표 임원진", requiredMode = REQUIRED)
    InnerRepresentative representative
) {

    public record InnerRecruitment(
        @Schema(description = "동아리 모집 상태", example = "ONGOING", requiredMode = REQUIRED)
        RecruitmentStatus status,

        @Schema(description = "동아리 모집 시작일", example = "2025-11-30", requiredMode = NOT_REQUIRED)
        LocalDate startDate,

        @Schema(description = "동아리 모집 마감일", example = "2025-12-31", requiredMode = NOT_REQUIRED)
        LocalDate endDate
    ) {

        public static InnerRecruitment from(ClubRecruitment clubRecruitment) {
            RecruitmentStatus status = RecruitmentStatus.of(clubRecruitment);

            LocalDate startDate = (clubRecruitment != null) ? clubRecruitment.getStartDate() : null;
            LocalDate endDate = (clubRecruitment != null) ? clubRecruitment.getEndDate() : null;

            return new InnerRecruitment(status, startDate, endDate);
        }
    }

    public record InnerRepresentative(
        @Schema(description = "동아리 대표 임원진 이름", example = "김철수", requiredMode = REQUIRED)
        String name,

        @Schema(description = "동아리 대표 임원진 전화번호", example = "01012345678", requiredMode = REQUIRED)
        String phone,

        @Schema(description = "동아리 대표 임원진 이메일", example = "example@koreatech.ac.kr", requiredMode = REQUIRED)
        String email
    ) {

        public static InnerRepresentative from(ClubExecutive clubExecutive) {
            User user = clubExecutive.getUser();

            return new InnerRepresentative(user.getName(), user.getPhoneNumber(), user.getEmail());
        }
    }

    public static ClubDetailResponse of(Club club, Long memberCount, ClubRecruitment clubRecruitment, ClubExecutive clubExecutive) {
        return new ClubDetailResponse(
            club.getId(),
            club.getName(),
            club.getDescription(),
            club.getIntroduce(),
            club.getImageUrl(),
            club.getClubCategory().getName(),
            memberCount,
            InnerRecruitment.from(clubRecruitment),
            InnerRepresentative.from(clubExecutive)
        );
    }
}
