package gg.agit.konect.unit.admin.website.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportConfirmRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportPreviewResponse;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportResponse;
import gg.agit.konect.admin.website.service.AdminWebsiteClubSheetImportService;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.model.WebUniversity;
import gg.agit.konect.domain.website.repository.WebClubRepository;
import gg.agit.konect.domain.website.repository.WebUniversityRepository;
import gg.agit.konect.domain.website.service.WebsiteClubStatsReader;
import gg.agit.konect.support.ServiceTestSupport;

class AdminWebsiteClubSheetImportServiceTest extends ServiceTestSupport {

    private static final Integer UNIVERSITY_ID = 1;

    @Mock
    private Sheets googleSheetsService;

    @Mock
    private Sheets.Spreadsheets spreadsheets;

    @Mock
    private Sheets.Spreadsheets.Values values;

    @Mock
    private Sheets.Spreadsheets.Values.Get getRequest;

    @Mock
    private WebUniversityRepository webUniversityRepository;

    @Mock
    private WebClubRepository webClubRepository;

    @Mock
    private WebsiteClubStatsReader websiteClubStatsReader;

    private AdminWebsiteClubSheetImportService service;

    @BeforeEach
    void setUp() {
        service = new AdminWebsiteClubSheetImportService(
            googleSheetsService,
            webUniversityRepository,
            webClubRepository,
            websiteClubStatsReader
        );
    }

    @Test
    void previewClubsReadsFixedSheetColumns() throws Exception {
        given(webUniversityRepository.getById(UNIVERSITY_ID)).willReturn(university());
        given(googleSheetsService.spreadsheets()).willReturn(spreadsheets);
        given(spreadsheets.values()).willReturn(values);
        given(values.get("sheet-id", "'작성 시트'!A1:F1000")).willReturn(getRequest);
        given(getRequest.setValueRenderOption("FORMATTED_VALUE")).willReturn(getRequest);
        given(getRequest.execute()).willReturn(new ValueRange().setValues(List.of(
            List.of("title"),
            List.of("description"),
            List.of(),
            List.of("동아리명", "동아리 분과", "기타 분과", "동아리 주제", "대표 이모지", "한 줄 소개"),
            List.of("예시) BCSD", "학술분과", "", "dev", "IT", "example"),
            List.of("BCSD", "학술분과", "", "dev", "IT", "dev club"),
            List.of("농구동아리", "체육(운동)분과", "", "", "", "")
        )));

        AdminWebsiteClubSheetImportPreviewResponse preview = service.previewClubs(
            UNIVERSITY_ID,
            "https://docs.google.com/spreadsheets/d/sheet-id/edit"
        );

        assertThat(preview.previewCount()).isEqualTo(2);
        assertThat(preview.clubs())
            .extracting(AdminWebsiteClubSheetImportPreviewResponse.PreviewClub::name)
            .containsExactly("BCSD", "농구동아리");
        assertThat(preview.clubs())
            .extracting(AdminWebsiteClubSheetImportPreviewResponse.PreviewClub::clubCategory)
            .containsExactly(ClubCategory.ACADEMIC, ClubCategory.SPORTS);
        assertThat(preview.clubs().get(1).topic()).isEqualTo("기타");
        assertThat(preview.clubs().get(1).categoryEmoji()).isEqualTo("⚽");
        assertThat(preview.clubs().get(1).description()).isEqualTo("농구동아리 동아리입니다.");
        assertThat(preview.clubs())
            .extracting(AdminWebsiteClubSheetImportPreviewResponse.PreviewClub::introduce)
            .containsExactly("", "");
        assertThat(preview.warnings()).hasSize(3);
    }

    @Test
    void confirmImportSavesEnabledAndNonDuplicateClubsOnly() {
        List<AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub> clubs = List.of(
            confirmClub(5, "BCSD", ClubCategory.ACADEMIC, true),
            confirmClub(6, "농구동아리", ClubCategory.SPORTS, true),
            confirmClub(7, "댄스동아리", ClubCategory.PERFORMANCE, false),
            confirmClub(8, "BCSD", ClubCategory.ACADEMIC, true)
        );

        given(webUniversityRepository.getById(UNIVERSITY_ID)).willReturn(university());
        given(webClubRepository.findExistingNamesByUniversityId(eq(UNIVERSITY_ID), anySet()))
            .willReturn(Set.of("농구동아리"));
        given(webClubRepository.saveAll(org.mockito.ArgumentMatchers.<List<WebClub>>any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        AdminWebsiteClubSheetImportResponse response = service.confirmImport(UNIVERSITY_ID, clubs);

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(2);
        assertThat(response.warnings()).hasSize(2);
        assertThat(response.warnings()).anyMatch(warning -> warning.contains("농구동아리"));
        assertThat(response.warnings()).anyMatch(warning -> warning.contains("BCSD"));
        verify(webClubRepository).saveAll(org.mockito.ArgumentMatchers.<List<WebClub>>argThat(savedClubs ->
            savedClubs.size() == 1
                && savedClubs.getFirst().getName().equals("BCSD")
                && savedClubs.getFirst().getIntroduce().isEmpty()
        ));
        verify(websiteClubStatsReader).invalidateUniversity(UNIVERSITY_ID);
        verifyNoInteractions(googleSheetsService);
    }

    @Test
    void confirmImportInvalidatesStatsAfterTransactionCommit() {
        List<AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub> clubs = List.of(
            confirmClub(5, "BCSD", ClubCategory.ACADEMIC, true)
        );

        given(webUniversityRepository.getById(UNIVERSITY_ID)).willReturn(university());
        given(webClubRepository.findExistingNamesByUniversityId(eq(UNIVERSITY_ID), anySet()))
            .willReturn(Set.of());
        given(webClubRepository.saveAll(org.mockito.ArgumentMatchers.<List<WebClub>>any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.confirmImport(UNIVERSITY_ID, clubs);

            verify(websiteClubStatsReader, never()).invalidateUniversity(UNIVERSITY_ID);
            TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
            verify(websiteClubStatsReader).invalidateUniversity(UNIVERSITY_ID);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void confirmImportSkipsExistingClubNameCaseInsensitively() {
        given(webUniversityRepository.getById(UNIVERSITY_ID)).willReturn(university());
        given(webClubRepository.findExistingNamesByUniversityId(eq(UNIVERSITY_ID), anySet()))
            .willReturn(Set.of("bcsd"));

        AdminWebsiteClubSheetImportResponse response = service.confirmImport(
            UNIVERSITY_ID,
            List.of(confirmClub(5, "BCSD", ClubCategory.ACADEMIC, true))
        );

        assertThat(response.importedCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.warnings()).singleElement()
            .asString()
            .contains("BCSD");
        verify(webClubRepository, org.mockito.Mockito.never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verifyNoInteractions(googleSheetsService);
    }

    @Test
    void previewClubsDisablesSuspiciousRows() throws Exception {
        given(webUniversityRepository.getById(UNIVERSITY_ID)).willReturn(university());
        given(googleSheetsService.spreadsheets()).willReturn(spreadsheets);
        given(spreadsheets.values()).willReturn(values);
        given(values.get("sheet-id", "'작성 시트'!A1:F1000")).willReturn(getRequest);
        given(getRequest.setValueRenderOption("FORMATTED_VALUE")).willReturn(getRequest);
        given(getRequest.execute()).willReturn(new ValueRange().setValues(List.of(
            List.of("title"),
            List.of("description"),
            List.of(),
            List.of("동아리명", "동아리 분과", "기타 분과", "동아리 주제", "대표 이모지", "한 줄 소개"),
            List.of("즐겁게 농구하는 중앙 농구 동아리입니다", "체육(운동)분과", "", "농구", "🏀", "농구 동아리"),
            List.of("BCSD", "학술분과", "", "미확인", "IT", "개발 동아리"),
            List.of("ZEST", "공연분과", "", "댄스", "🎭", "문의 https://example.com")
        )));

        AdminWebsiteClubSheetImportPreviewResponse preview = service.previewClubs(
            UNIVERSITY_ID,
            "https://docs.google.com/spreadsheets/d/sheet-id/edit"
        );

        assertThat(preview.clubs())
            .extracting(AdminWebsiteClubSheetImportPreviewResponse.PreviewClub::enabled)
            .containsExactly(false, false, false);
        assertThat(preview.warnings())
            .anyMatch(warning -> warning.contains("5행") && warning.contains("동아리명"))
            .anyMatch(warning -> warning.contains("6행") && warning.contains("동아리 주제"))
            .anyMatch(warning -> warning.contains("7행") && warning.contains("한 줄 소개"));
    }

    @Test
    void confirmImportSkipsSuspiciousEnabledClub() {
        given(webUniversityRepository.getById(UNIVERSITY_ID)).willReturn(university());
        given(webClubRepository.findExistingNamesByUniversityId(eq(UNIVERSITY_ID), anySet())).willReturn(Set.of());

        AdminWebsiteClubSheetImportResponse response = service.confirmImport(
            UNIVERSITY_ID,
            List.of(new AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub(
                5,
                "즐겁게 농구하는 중앙 농구 동아리입니다",
                ClubCategory.SPORTS,
                "농구",
                "농구 동아리",
                "",
                "🏀",
                true
            ))
        );

        assertThat(response.importedCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.warnings()).singleElement()
            .asString()
            .contains("동아리명");
        verify(webClubRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verifyNoInteractions(googleSheetsService);
    }

    private WebUniversity university() {
        return WebUniversity.builder()
            .id(UNIVERSITY_ID)
            .koreanName("한국기술교육대학교")
            .campus(Campus.MAIN)
            .region(UniversityRegion.CHUNGCHEONG)
            .imageUrl("https://example.com/logo.png")
            .build();
    }

    private AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub confirmClub(
        int rowNumber,
        String name,
        ClubCategory category,
        boolean enabled
    ) {
        return new AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub(
            rowNumber,
            name,
            category,
            "dev",
            "dev club",
            "",
            "IT",
            enabled
        );
    }
}
