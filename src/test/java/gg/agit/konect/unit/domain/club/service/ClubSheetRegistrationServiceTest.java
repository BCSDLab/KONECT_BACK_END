package gg.agit.konect.unit.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubSheetRegistrationService;
import gg.agit.konect.domain.club.service.SheetHeaderMapper;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
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

    @Test
    @DisplayName("회원 목록 매핑이 null이면 시트 정보를 저장하지 않는다")
    void updateSheetRegistrationThrowsWhenMemberListMappingIsNull() {
        // given
        Integer clubId = 1;
        String spreadsheetId = "spreadsheet-id";
        Club club = ClubFixture.create(UniversityFixture.create());
        SheetHeaderMapper.SheetAnalysisResult analysisResult = new SheetHeaderMapper.SheetAnalysisResult(
            null,
            null,
            null
        );

        given(clubRepository.getById(clubId)).willReturn(club);

        // when & then
        assertThatThrownBy(() -> clubSheetRegistrationService.updateSheetRegistration(
            clubId,
            spreadsheetId,
            analysisResult
        ))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(
                ApiResponseCode.CLUB_SHEET_ANALYSIS_REQUIRED));

        assertThat(club.getGoogleSheetId()).isNull();
        assertThat(club.getSheetColumnMapping()).isNull();
        verify(clubRepository, never()).save(club);
    }

    @Test
    @DisplayName("매핑 직렬화에 실패하면 시트 정보를 저장하지 않는다")
    void updateSheetRegistrationThrowsWhenMappingSerializationFails() throws Exception {
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
        given(objectMapper.writeValueAsString(mapping.toMap())).willThrow(new JsonProcessingException("boom") {});

        // when & then
        assertThatThrownBy(() -> clubSheetRegistrationService.updateSheetRegistration(
            clubId,
            spreadsheetId,
            analysisResult
        ))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(
                ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET));

        assertThat(club.getGoogleSheetId()).isNull();
        assertThat(club.getSheetColumnMapping()).isNull();
        verify(clubRepository, never()).save(club);
    }
}
