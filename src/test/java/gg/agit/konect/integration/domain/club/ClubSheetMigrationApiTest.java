package gg.agit.konect.integration.domain.club;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import gg.agit.konect.domain.club.dto.SheetImportRequest;
import gg.agit.konect.domain.club.dto.SheetImportResponse;
import gg.agit.konect.domain.club.service.ClubSheetIntegratedService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;

class ClubSheetMigrationApiTest extends IntegrationTestSupport {

    @MockitoBean
    private ClubSheetIntegratedService clubSheetIntegratedService;

    private static final Integer CLUB_ID = 1;
    private static final Integer REQUESTER_ID = 100;
    private static final String SPREADSHEET_URL =
        "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit";

    @BeforeEach
    void setUp() throws Exception {
        mockLoginUser(REQUESTER_ID);
    }

    @Nested
    @DisplayName("POST /clubs/{clubId}/sheet/import/integrated - 시트 통합 가져오기")
    class AnalyzeAndImportPreMembers {

        @Test
        @DisplayName("시트 분석 등록 후 사전 회원 가져오기 결과를 반환한다")
        void analyzeAndImportPreMembersSuccess() throws Exception {
            // given
            given(clubSheetIntegratedService.analyzeAndImportPreMembers(
                eq(CLUB_ID),
                eq(REQUESTER_ID),
                eq(SPREADSHEET_URL)
            )).willReturn(SheetImportResponse.of(2, 1, List.of("전화번호 형식 경고")));

            SheetImportRequest request = new SheetImportRequest(SPREADSHEET_URL);

            // when & then
            performPost("/clubs/" + CLUB_ID + "/sheet/import/integrated", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(2))
                .andExpect(jsonPath("$.autoRegisteredCount").value(1))
                .andExpect(jsonPath("$.warnings[0]").value("전화번호 형식 경고"));
        }

        @Test
        @DisplayName("구글 스프레드시트 403 오류를 response body로 반환한다")
        void analyzeAndImportPreMembersForbiddenGoogleSheetAccess() throws Exception {
            // given
            given(clubSheetIntegratedService.analyzeAndImportPreMembers(
                eq(CLUB_ID),
                eq(REQUESTER_ID),
                eq(SPREADSHEET_URL)
            )).willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS));

            SheetImportRequest request = new SheetImportRequest(SPREADSHEET_URL);

            // when & then
            performPost("/clubs/" + CLUB_ID + "/sheet/import/integrated", request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                    .value(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS.name()))
                .andExpect(jsonPath("$.message")
                    .value(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS.getMessage()));
        }
    }
}
