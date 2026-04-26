package gg.agit.konect.unit.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubSheetRegistrationService;
import gg.agit.konect.domain.club.service.SheetHeaderMapper;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;

class ClubSheetRegistrationServiceTest extends ServiceTestSupport {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ClubSheetRegistrationService clubSheetRegistrationService;

    @Test
    @DisplayName("시트 등록 정보를 트랜잭션 서비스에서 저장한다")
    void updateSheetRegistrationUpdatesClubSheetInfo() throws Exception {
        // given
        Integer clubId = 1;
        String spreadsheetId = "spreadsheet-id";
        Club club = ClubFixture.create(UniversityFixture.create());
        SheetColumnMapping mapping = SheetColumnMapping.defaultMapping();
        SheetHeaderMapper.SheetAnalysisResult analysisResult = new SheetHeaderMapper.SheetAnalysisResult(
            mapping,
            null,
            null
        );

        given(clubRepository.getById(clubId)).willReturn(club);
        given(objectMapper.writeValueAsString(mapping.toMap())).willReturn("{}");

        // when
        clubSheetRegistrationService.updateSheetRegistration(clubId, spreadsheetId, analysisResult);

        // then
        assertThat(club.getGoogleSheetId()).isEqualTo(spreadsheetId);
        assertThat(club.getSheetColumnMapping()).isEqualTo("{}");
        verify(clubRepository).save(club);
    }
}
