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

import gg.agit.konect.domain.club.dto.SheetImportConfirmRequest;
import gg.agit.konect.domain.club.dto.SheetImportPreviewResponse;
import gg.agit.konect.domain.club.dto.SheetImportRequest;
import gg.agit.konect.domain.club.dto.SheetImportResponse;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.service.ClubSheetIntegratedService;
import gg.agit.konect.domain.club.service.SheetImportService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;

class ClubSheetMigrationApiTest extends IntegrationTestSupport {

    @MockitoBean
    private SheetImportService sheetImportService;

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
    @DisplayName("POST /clubs/{clubId}/sheet/import/preview")
    class PreviewPreMembers {

        @Test
        @DisplayName("returns preview member list")
        void previewPreMembersSuccess() throws Exception {
            SheetImportPreviewResponse response = SheetImportPreviewResponse.of(
                List.of(
                    new SheetImportPreviewResponse.PreviewMember(
                        "2021232948",
                        "Kim Manager",
                        ClubPosition.MANAGER,
                        true,
                        true
                    ),
                    new SheetImportPreviewResponse.PreviewMember(
                        "2021232949",
                        "Lee Member",
                        ClubPosition.MEMBER,
                        false,
                        true
                    )
                ),
                List.of("전화번호 형식 경고")
            );

            given(sheetImportService.previewPreMembersFromSheet(
                eq(CLUB_ID),
                eq(REQUESTER_ID)
            )).willReturn(response);

            performPost("/clubs/" + CLUB_ID + "/sheet/import/preview")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previewCount").value(2))
                .andExpect(jsonPath("$.autoRegisteredCount").value(1))
                .andExpect(jsonPath("$.preRegisteredCount").value(1))
                .andExpect(jsonPath("$.members[0].studentNumber").value("2021232948"))
                .andExpect(jsonPath("$.members[0].isDirectMember").value(true))
                .andExpect(jsonPath("$.members[0].enabled").value(true))
                .andExpect(jsonPath("$.members[1].studentNumber").value("2021232949"))
                .andExpect(jsonPath("$.members[1].isDirectMember").value(false))
                .andExpect(jsonPath("$.members[1].enabled").value(true))
                .andExpect(jsonPath("$.warnings[0]").value("전화번호 형식 경고"));
        }

        @Test
        @DisplayName("returns 403 when sheet access is forbidden")
        void previewPreMembersForbiddenGoogleSheetAccess() throws Exception {
            given(sheetImportService.previewPreMembersFromSheet(
                eq(CLUB_ID),
                eq(REQUESTER_ID)
            )).willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS));

            performPost("/clubs/" + CLUB_ID + "/sheet/import/preview")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                    .value(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS.name()))
                .andExpect(jsonPath("$.message")
                    .value(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS.getMessage()));
        }

        @Test
        @DisplayName("returns 400 when sheet analysis and registration are not completed")
        void previewPreMembersRequiresRegisteredSheet() throws Exception {
            given(sheetImportService.previewPreMembersFromSheet(
                eq(CLUB_ID),
                eq(REQUESTER_ID)
            )).willThrow(CustomException.of(ApiResponseCode.CLUB_SHEET_ANALYSIS_REQUIRED));

            performPost("/clubs/" + CLUB_ID + "/sheet/import/preview")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                    .value(ApiResponseCode.CLUB_SHEET_ANALYSIS_REQUIRED.name()))
                .andExpect(jsonPath("$.message")
                    .value(ApiResponseCode.CLUB_SHEET_ANALYSIS_REQUIRED.getMessage()));
        }
    }

    @Nested
    @DisplayName("POST /clubs/{clubId}/sheet/import/confirm")
    class ConfirmImportPreMembers {

        @Test
        @DisplayName("imports only enabled preview members")
        void confirmImportPreMembersSuccess() throws Exception {
            given(sheetImportService.confirmImportPreMembers(
                eq(CLUB_ID),
                eq(REQUESTER_ID),
                eq(List.of(
                    new SheetImportConfirmRequest.ConfirmMember(
                        "2021232948",
                        "Kim Manager",
                        ClubPosition.MANAGER,
                        true
                    ),
                    new SheetImportConfirmRequest.ConfirmMember(
                        "2021232949",
                        "Lee Member",
                        ClubPosition.MEMBER,
                        false
                    ),
                    new SheetImportConfirmRequest.ConfirmMember(
                        "2021232950",
                        "Park Member",
                        ClubPosition.MEMBER,
                        true
                    )
                ))
            )).willReturn(SheetImportResponse.of(1, 1, List.of()));

            SheetImportConfirmRequest request = new SheetImportConfirmRequest(List.of(
                new SheetImportConfirmRequest.ConfirmMember(
                    "2021232948",
                    "Kim Manager",
                    ClubPosition.MANAGER,
                    true
                ),
                new SheetImportConfirmRequest.ConfirmMember(
                    "2021232949",
                    "Lee Member",
                    ClubPosition.MEMBER,
                    false
                ),
                new SheetImportConfirmRequest.ConfirmMember(
                    "2021232950",
                    "Park Member",
                    ClubPosition.MEMBER,
                    true
                )
            ));

            performPost("/clubs/" + CLUB_ID + "/sheet/import/confirm", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.autoRegisteredCount").value(1));
        }

        @Test
        @DisplayName("returns 403 when confirm import access is forbidden")
        void confirmImportPreMembersForbidden() throws Exception {
            SheetImportConfirmRequest request = new SheetImportConfirmRequest(List.of(
                new SheetImportConfirmRequest.ConfirmMember(
                    "2021232948",
                    "Kim Manager",
                    ClubPosition.MANAGER,
                    true
                )
            ));

            given(sheetImportService.confirmImportPreMembers(
                eq(CLUB_ID),
                eq(REQUESTER_ID),
                eq(request.members())
            )).willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS));

            performPost("/clubs/" + CLUB_ID + "/sheet/import/confirm", request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                    .value(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS.name()))
                .andExpect(jsonPath("$.message")
                    .value(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS.getMessage()));
        }
    }

    @Nested
    @DisplayName("POST /clubs/{clubId}/sheet/import/integrated")
    class AnalyzeAndImportPreMembers {

        @Test
        @DisplayName("returns integrated import result")
        void analyzeAndImportPreMembersSuccess() throws Exception {
            given(clubSheetIntegratedService.analyzeAndImportPreMembers(
                eq(CLUB_ID),
                eq(REQUESTER_ID),
                eq(SPREADSHEET_URL)
            )).willReturn(SheetImportResponse.of(2, 1, List.of("전화번호 형식 경고")));

            SheetImportRequest request = new SheetImportRequest(SPREADSHEET_URL);

            performPost("/clubs/" + CLUB_ID + "/sheet/import/integrated", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(2))
                .andExpect(jsonPath("$.autoRegisteredCount").value(1))
                .andExpect(jsonPath("$.warnings[0]").value("전화번호 형식 경고"));
        }

        @Test
        @DisplayName("returns 403 response body for forbidden sheet access")
        void analyzeAndImportPreMembersForbiddenGoogleSheetAccess() throws Exception {
            given(clubSheetIntegratedService.analyzeAndImportPreMembers(
                eq(CLUB_ID),
                eq(REQUESTER_ID),
                eq(SPREADSHEET_URL)
            )).willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS));

            SheetImportRequest request = new SheetImportRequest(SPREADSHEET_URL);

            performPost("/clubs/" + CLUB_ID + "/sheet/import/integrated", request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                    .value(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS.name()))
                .andExpect(jsonPath("$.message")
                    .value(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS.getMessage()));
        }

        @Test
        @DisplayName("returns detail for invalid Google Drive auth")
        void analyzeAndImportPreMembersInvalidGoogleDriveAuth() throws Exception {
            String detail =
                "400 Bad Request\nPOST https://oauth2.googleapis.com/token\n{\"error\":\"invalid_grant\"}";
            given(clubSheetIntegratedService.analyzeAndImportPreMembers(
                eq(CLUB_ID),
                eq(REQUESTER_ID),
                eq(SPREADSHEET_URL)
            )).willThrow(CustomException.of(ApiResponseCode.INVALID_GOOGLE_DRIVE_AUTH, detail));

            SheetImportRequest request = new SheetImportRequest(SPREADSHEET_URL);

            performPost("/clubs/" + CLUB_ID + "/sheet/import/integrated", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                    .value(ApiResponseCode.INVALID_GOOGLE_DRIVE_AUTH.name()))
                .andExpect(jsonPath("$.message")
                    .value(ApiResponseCode.INVALID_GOOGLE_DRIVE_AUTH.getMessage()))
                .andExpect(jsonPath("$.detail").value(detail));
        }
    }
}
