package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMemberSheetSyncResponse(
    @Schema(description = "동기화 요청된 회원 및 사전 회원 수", example = "42")
    int syncedMemberCount,

    @Schema(
        description = "동기화된 스프레드시트 URL",
        example = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit"
    )
    String sheetUrl
) {
    public static ClubMemberSheetSyncResponse of(int syncedMemberCount, String spreadsheetId) {
        String sheetUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit";
        return new ClubMemberSheetSyncResponse(syncedMemberCount, sheetUrl);
    }
}
