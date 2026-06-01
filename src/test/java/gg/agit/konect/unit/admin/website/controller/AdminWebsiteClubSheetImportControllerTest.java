package gg.agit.konect.unit.admin.website.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;

import gg.agit.konect.admin.website.controller.AdminWebsiteClubSheetImportController;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportConfirmRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportPreviewResponse;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportResponse;
import gg.agit.konect.admin.website.service.AdminWebsiteClubSheetImportService;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.support.ServiceTestSupport;

class AdminWebsiteClubSheetImportControllerTest extends ServiceTestSupport {

    private static final Integer UNIVERSITY_ID = 1;
    private static final String SPREADSHEET_URL = "https://docs.google.com/spreadsheets/d/sheet-id/edit";

    @Mock
    private AdminWebsiteClubSheetImportService service;

    @InjectMocks
    private AdminWebsiteClubSheetImportController controller;

    @Test
    void previewClubsReturnsServiceResponse() {
        AdminWebsiteClubSheetImportPreviewResponse serviceResponse =
            AdminWebsiteClubSheetImportPreviewResponse.of(
                UNIVERSITY_ID,
                List.of(new AdminWebsiteClubSheetImportPreviewResponse.PreviewClub(
                    5,
                    "BCSD",
                    ClubCategory.ACADEMIC,
                    "dev",
                    "dev club",
                    "dev club",
                    "IT",
                    true
                )),
                List.of()
            );
        given(service.previewClubs(UNIVERSITY_ID, SPREADSHEET_URL)).willReturn(serviceResponse);

        ResponseEntity<AdminWebsiteClubSheetImportPreviewResponse> response = controller.previewClubs(
            UNIVERSITY_ID,
            new AdminWebsiteClubSheetImportRequest(SPREADSHEET_URL)
        );

        assertThat(response.getBody()).isSameAs(serviceResponse);
    }

    @Test
    void confirmImportReturnsServiceResponse() {
        AdminWebsiteClubSheetImportResponse serviceResponse =
            AdminWebsiteClubSheetImportResponse.of(1, 0, List.of());
        AdminWebsiteClubSheetImportConfirmRequest request =
            new AdminWebsiteClubSheetImportConfirmRequest(List.of(
                new AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub(
                    5,
                    "BCSD",
                    ClubCategory.ACADEMIC,
                    "dev",
                    "dev club",
                    "dev club",
                    "IT",
                    true
                )
            ));
        given(service.confirmImport(UNIVERSITY_ID, request.clubs())).willReturn(serviceResponse);

        ResponseEntity<AdminWebsiteClubSheetImportResponse> response =
            controller.confirmImport(UNIVERSITY_ID, request);

        assertThat(response.getBody()).isSameAs(serviceResponse);
    }
}
