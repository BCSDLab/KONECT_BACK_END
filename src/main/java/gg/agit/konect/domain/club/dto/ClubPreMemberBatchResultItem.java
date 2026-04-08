package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.global.code.ApiResponseCode;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubPreMemberBatchResultItem(
    @Schema(description = "학번", example = "2021136089", requiredMode = REQUIRED)
    String studentNumber,

    @Schema(description = "이름", example = "홍길동", requiredMode = REQUIRED)
    String name,

    @Schema(description = "성공 여부", example = "true", requiredMode = REQUIRED)
    Boolean success,

    // 성공 시 반환되는 필드
    @Schema(description = "동아리 고유 ID (성공 시)", example = "1")
    Integer clubId,

    @Schema(description = "가입 직책 (성공 시)", example = "MEMBER")
    ClubPosition clubPosition,

    @Schema(description = "직접 회원 가입 여부 (성공 시: true면 ClubMember 직접 추가, false면 ClubPreMember 사전등록)",
        example = "false")
    Boolean isDirectMember,

    // 실패 시 반환되는 필드
    @Schema(description = "오류 코드 (실패 시)", example = "ALREADY_CLUB_MEMBER")
    String errorCode,

    @Schema(description = "오류 메시지 (실패 시)", example = "이미 동아리 회원입니다.")
    String errorMessage
) {

    public static ClubPreMemberBatchResultItem success(ClubPreMemberAddRequest request,
        ClubPreMemberAddResponse response) {
        return new ClubPreMemberBatchResultItem(
            request.studentNumber(),
            request.name(),
            true,
            response.clubId(),
            response.clubPosition(),
            response.isDirectMember(),
            null,
            null
        );
    }

    public static ClubPreMemberBatchResultItem fail(ClubPreMemberAddRequest request,
        ApiResponseCode errorCode) {
        return new ClubPreMemberBatchResultItem(
            request.studentNumber(),
            request.name(),
            false,
            null,
            null,
            null,
            errorCode.name(),
            errorCode.getMessage()
        );
    }
}
