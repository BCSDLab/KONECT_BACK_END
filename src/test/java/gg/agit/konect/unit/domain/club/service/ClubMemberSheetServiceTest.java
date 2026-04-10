package gg.agit.konect.unit.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_SHEET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubMemberSheetService;
import gg.agit.konect.domain.club.service.ClubPermissionValidator;
import gg.agit.konect.domain.club.service.SheetHeaderMapper;
import gg.agit.konect.domain.club.service.SheetSyncExecutor;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;

class ClubMemberSheetServiceTest extends ServiceTestSupport {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubPreMemberRepository clubPreMemberRepository;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @Mock
    private SheetSyncExecutor sheetSyncExecutor;

    @Mock
    private SheetHeaderMapper sheetHeaderMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ClubMemberSheetService clubMemberSheetService;

    @Test
    @DisplayName("시트 동기화 수에 사전 회원도 포함한다")
    void syncMembersToSheetIncludesPreMembersInCount() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetId = "spreadsheet-id";
        Club club = ClubFixture.create(UniversityFixture.create());
        club.updateGoogleSheetId(spreadsheetId);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(clubMemberRepository.countByClubId(clubId)).willReturn(2L);
        given(clubPreMemberRepository.countByClubId(clubId)).willReturn(3L);

        // when
        ClubMemberSheetSyncResponse response = clubMemberSheetService.syncMembersToSheet(
            clubId,
            requesterId,
            ClubSheetSortKey.POSITION,
            true
        );

        // then
        verify(clubPermissionValidator).validateManagerAccess(clubId, requesterId);
        verify(sheetSyncExecutor).executeWithSort(clubId, ClubSheetSortKey.POSITION, true);
        assertThat(response.syncedMemberCount()).isEqualTo(5);
        assertThat(response.sheetUrl())
            .isEqualTo("https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit");
    }

    @Test
    @DisplayName("updateSheetId는 정상 동작한다")
    void updateSheetIdWorksNormally() throws JsonProcessingException {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetUrl = "https://docs.google.com/spreadsheets/d/test-sheet-id/edit";
        Club club = ClubFixture.create(UniversityFixture.create());
        ClubSheetIdUpdateRequest request = new ClubSheetIdUpdateRequest(spreadsheetUrl);
        gg.agit.konect.domain.club.model.SheetColumnMapping mapping = gg.agit.konect.domain.club.model.SheetColumnMapping.defaultMapping();
        SheetHeaderMapper.SheetAnalysisResult analysisResult = new SheetHeaderMapper.SheetAnalysisResult(
            mapping,
            null,
            null
        );

        given(clubRepository.getById(clubId)).willReturn(club);
        given(sheetHeaderMapper.analyzeAllSheets("test-sheet-id")).willReturn(analysisResult);
        given(objectMapper.writeValueAsString(analysisResult.memberListMapping().toMap())).willReturn("{}");

        // when
        clubMemberSheetService.updateSheetId(clubId, requesterId, request);

        // then
        verify(clubPermissionValidator).validateManagerAccess(clubId, requesterId);
        verify(sheetHeaderMapper).analyzeAllSheets("test-sheet-id");
        assertThat(club.getGoogleSheetId()).isEqualTo("test-sheet-id");
    }

    @Test
    @DisplayName("syncMembersToSheet는 sheetId가 null인 경우 NOT_FOUND_CLUB_SHEET_ID 예외를 던진다")
    void syncMembersToSheetThrowsNotFoundClubSheetIdWhenSheetIdIsNull() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        Club club = ClubFixture.create(UniversityFixture.create());

        given(clubRepository.getById(clubId)).willReturn(club);

        // when & then
        assertThatThrownBy(() -> clubMemberSheetService.syncMembersToSheet(
            clubId,
            requesterId,
            ClubSheetSortKey.POSITION,
            true
        ))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode()).isEqualTo(NOT_FOUND_CLUB_SHEET_ID));
    }

    @Test
    @DisplayName("syncMembersToSheet는 sheetId가 blank인 경우 NOT_FOUND_CLUB_SHEET_ID 예외를 던진다")
    void syncMembersToSheetThrowsNotFoundClubSheetIdWhenSheetIdIsBlank() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        Club club = ClubFixture.create(UniversityFixture.create());
        club.updateGoogleSheetId("   ");

        given(clubRepository.getById(clubId)).willReturn(club);

        // when & then
        assertThatThrownBy(() -> clubMemberSheetService.syncMembersToSheet(
            clubId,
            requesterId,
            ClubSheetSortKey.POSITION,
            true
        ))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode()).isEqualTo(NOT_FOUND_CLUB_SHEET_ID));
    }
}
