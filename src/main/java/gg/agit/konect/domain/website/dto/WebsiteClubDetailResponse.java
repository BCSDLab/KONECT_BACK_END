package gg.agit.konect.domain.website.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import io.swagger.v3.oas.annotations.media.Schema;

public record WebsiteClubDetailResponse(
    @Schema(description = "동아리 고유 ID", example = "1", requiredMode = REQUIRED)
    Integer id,

    @Schema(description = "동아리명", example = "BCSD Lab", requiredMode = REQUIRED)
    String name,

    @Schema(description = "동아리 로고 이미지 URL", requiredMode = REQUIRED)
    String imageUrl,

    @Schema(description = "분과 코드", example = "ACADEMIC", requiredMode = REQUIRED)
    ClubCategory category,

    @Schema(description = "분과명", example = "학술", requiredMode = REQUIRED)
    String categoryName,

    @Schema(description = "한 줄 소개", example = "테스트 동아리 소개", requiredMode = REQUIRED)
    String description,

    @Schema(description = "상세 소개", requiredMode = REQUIRED)
    String introduce,

    @Schema(description = "활동 위치", example = "학생회관 101호", requiredMode = REQUIRED)
    String location,

    @Schema(description = "등록 회원 수", example = "31", requiredMode = REQUIRED)
    Long memberCount,

    @Schema(description = "대학 정보", requiredMode = REQUIRED)
    University university,

    @Schema(description = "모집 정보", requiredMode = REQUIRED)
    Recruitment recruitment
) {

    public record University(
        @Schema(description = "대학 고유 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "대학명", example = "한국기술교육대학교", requiredMode = REQUIRED)
        String name,

        @Schema(description = "캠퍼스명", example = "본교", requiredMode = REQUIRED)
        String campusName,

        @Schema(description = "지역 코드", example = "CHUNGCHEONG", requiredMode = REQUIRED)
        UniversityRegion region,

        @Schema(description = "지역명", example = "충청도", requiredMode = REQUIRED)
        String regionName
    ) {
    }

    public record Recruitment(
        @Schema(description = "모집 활성화 여부", example = "true", requiredMode = REQUIRED)
        Boolean isRecruitmentEnabled,

        @Schema(description = "상시 모집 여부", example = "false", requiredMode = REQUIRED)
        Boolean isAlwaysRecruiting,

        @Schema(description = "모집 시작 일시", requiredMode = REQUIRED)
        LocalDateTime startAt,

        @Schema(description = "모집 마감 일시", requiredMode = REQUIRED)
        LocalDateTime endAt,

        @Schema(description = "모집 공고 내용", requiredMode = REQUIRED)
        String content
    ) {
        private static Recruitment from(Club club) {
            ClubRecruitment recruitment = club.getClubRecruitment();
            if (recruitment == null) {
                return new Recruitment(club.getIsRecruitmentEnabled(), false, null, null, null);
            }

            return new Recruitment(
                club.getIsRecruitmentEnabled(),
                recruitment.getIsAlwaysRecruiting(),
                recruitment.getStartAt(),
                recruitment.getEndAt(),
                recruitment.getContent()
            );
        }
    }

    public static WebsiteClubDetailResponse of(Club club, Long memberCount) {
        return new WebsiteClubDetailResponse(
            club.getId(),
            club.getName(),
            club.getImageUrl(),
            club.getClubCategory(),
            club.getClubCategory().getDescription(),
            club.getDescription(),
            club.getIntroduce(),
            club.getLocation(),
            memberCount,
            new University(
                club.getUniversity().getId(),
                club.getUniversity().getKoreanName(),
                club.getUniversity().getCampus().getDisplayName(),
                club.getUniversity().getRegion(),
                club.getUniversity().getRegion().getDisplayName()
            ),
            Recruitment.from(club)
        );
    }
}
