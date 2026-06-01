package gg.agit.konect.unit.admin.website.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;

import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportConfirmRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportResponse;
import gg.agit.konect.admin.website.service.AdminWebsiteClubSheetImportService;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.model.WebUniversity;
import gg.agit.konect.domain.website.repository.WebClubRepository;
import gg.agit.konect.domain.website.repository.WebUniversityRepository;
import gg.agit.konect.infrastructure.claude.config.ClaudeProperties;
import gg.agit.konect.support.ServiceTestSupport;

class AdminWebsiteClubSheetImportServiceTest extends ServiceTestSupport {

    private static final Integer UNIVERSITY_ID = 1;

    @Mock
    private Sheets googleSheetsService;

    @Mock
    private WebUniversityRepository webUniversityRepository;

    @Mock
    private WebClubRepository webClubRepository;

    private AdminWebsiteClubSheetImportService service;

    @BeforeEach
    void setUp() {
        service = new AdminWebsiteClubSheetImportService(
            googleSheetsService,
            new ClaudeProperties("test-api-key", "test-model"),
            new ObjectMapper(),
            RestClient.builder(),
            webUniversityRepository,
            webClubRepository
        );
    }

    @Test
    void confirmImportSavesEnabledAndNonDuplicateClubsOnly() {
        WebUniversity university = WebUniversity.builder()
            .id(UNIVERSITY_ID)
            .koreanName("한국기술교육대학교")
            .campus(Campus.MAIN)
            .region(UniversityRegion.CHUNGCHEONG)
            .imageUrl("https://example.com/logo.png")
            .build();

        List<AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub> clubs = List.of(
            new AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub(
                5,
                "BCSD",
                ClubCategory.ACADEMIC,
                "개발",
                "IT 동아리입니다.",
                "IT 동아리입니다.",
                "💻",
                true
            ),
            new AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub(
                6,
                "농구동아리",
                ClubCategory.SPORTS,
                "농구",
                "농구 동아리입니다.",
                "농구 동아리입니다.",
                "🏀",
                true
            ),
            new AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub(
                7,
                "댄스동아리",
                ClubCategory.PERFORMANCE,
                "댄스",
                "댄스 동아리입니다.",
                "댄스 동아리입니다.",
                "💃",
                false
            ),
            new AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub(
                8,
                "BCSD",
                ClubCategory.ACADEMIC,
                "개발",
                "중복 동아리입니다.",
                "중복 동아리입니다.",
                "💻",
                true
            )
        );

        given(webUniversityRepository.getById(UNIVERSITY_ID)).willReturn(university);
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
            savedClubs.size() == 1 && savedClubs.getFirst().getName().equals("BCSD")
        ));
        verifyNoInteractions(googleSheetsService);
    }

    @Test
    void confirmImportSkipsExistingClubNameCaseInsensitively() {
        WebUniversity university = WebUniversity.builder()
            .id(UNIVERSITY_ID)
            .koreanName("한국기술교육대학교")
            .campus(Campus.MAIN)
            .region(UniversityRegion.CHUNGCHEONG)
            .imageUrl("https://example.com/logo.png")
            .build();

        List<AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub> clubs = List.of(
            new AdminWebsiteClubSheetImportConfirmRequest.ConfirmClub(
                5,
                "BCSD",
                ClubCategory.ACADEMIC,
                "개발",
                "IT 동아리입니다.",
                "IT 동아리입니다.",
                "💻",
                true
            )
        );

        given(webUniversityRepository.getById(UNIVERSITY_ID)).willReturn(university);
        given(webClubRepository.findExistingNamesByUniversityId(eq(UNIVERSITY_ID), anySet()))
            .willReturn(Set.of("bcsd"));

        AdminWebsiteClubSheetImportResponse response = service.confirmImport(UNIVERSITY_ID, clubs);

        assertThat(response.importedCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.warnings()).singleElement()
            .asString()
            .contains("BCSD");
        verify(webClubRepository, org.mockito.Mockito.never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verifyNoInteractions(googleSheetsService);
    }
}
