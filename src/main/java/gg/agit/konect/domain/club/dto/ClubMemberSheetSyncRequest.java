package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ClubMemberSheetSyncRequest(
    @NotBlank(message = "스프레드시트 ID는 필수 입력입니다.")
    @Schema(
        description = "동기화 대상 구글 스프레드시트 ID (URL의 /d/{spreadsheetId}/ 부분)",
        example = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms"
    )
    String spreadsheetId
) {
}
