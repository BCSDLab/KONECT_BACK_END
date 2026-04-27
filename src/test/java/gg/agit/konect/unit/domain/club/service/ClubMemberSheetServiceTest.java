package gg.agit.konect.unit.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_SHEET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.club.service.ClubMemberSheetService;
import gg.agit.konect.domain.club.service.ClubPermissionValidator;
import gg.agit.konect.domain.club.service.ClubSheetRegistrationService;
import gg.agit.konect.domain.club.service.SheetHeaderMapper;
import gg.agit.konect.domain.club.service.SheetSyncExecutor;
import gg.agit.konect.global.code.ApiResponseCode;
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
    private ClubSheetRegistrationService clubSheetRegistrationService;

    @InjectMocks
    private ClubMemberSheetService clubMemberSheetService;

    @Test
    @DisplayName("시트 동기화 응답에 사전 회원 수를 포함한다")
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
    @DisplayName("시트 ID를 분석한 뒤 등록 서비스에 위임한다")
    void updateSheetIdWorksNormally() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetUrl = "https://docs.google.com/spreadsheets/d/test-sheet-id/edit";
        ClubSheetIdUpdateRequest request = new ClubSheetIdUpdateRequest(spreadsheetUrl);
        SheetColumnMapping mapping = SheetColumnMapping.defaultMapping();
        SheetHeaderMapper.SheetAnalysisResult analysisResult = new SheetHeaderMapper.SheetAnalysisResult(
            mapping,
            null,
            null
        );

        given(clubRepository.existsById(clubId)).willReturn(true);
        given(sheetHeaderMapper.analyzeAllSheets("test-sheet-id")).willReturn(analysisResult);

        // when
        clubMemberSheetService.updateSheetId(clubId, requesterId, request);

        // then
        verify(clubPermissionValidator).validateManagerAccess(clubId, requesterId);
        verify(sheetHeaderMapper).analyzeAllSheets("test-sheet-id");
        verify(clubSheetRegistrationService).updateSheetRegistration(clubId, "test-sheet-id", analysisResult);
    }

    @Test
    @DisplayName("updateSheetId는 동아리가 없으면 외부 분석을 호출하지 않는다")
    void updateSheetIdThrowsNotFoundClubBeforeSheetAnalysis() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetUrl = "invalid-sheet-url";
        ClubSheetIdUpdateRequest request = new ClubSheetIdUpdateRequest(spreadsheetUrl);

        given(clubRepository.existsById(clubId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> clubMemberSheetService.updateSheetId(clubId, requesterId, request))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(
                ApiResponseCode.NOT_FOUND_CLUB));

        verifyNoInteractions(clubPermissionValidator, sheetHeaderMapper, clubSheetRegistrationService);
    }

    @Test
    @DisplayName("syncMembersToSheet는 sheetId가 null이면 예외를 던진다")
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
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(
                NOT_FOUND_CLUB_SHEET_ID));
    }

    @Test
    @DisplayName("syncMembersToSheet는 sheetId가 blank이면 예외를 던진다")
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
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(
                NOT_FOUND_CLUB_SHEET_ID));
    }

    @Test
    @DisplayName("syncMembersToSheet는 빈 동아리도 정상 처리한다")
    void syncMembersToSheetHandlesEmptyClub() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetId = "spreadsheet-id";
        Club club = ClubFixture.create(UniversityFixture.create());
        club.updateGoogleSheetId(spreadsheetId);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(clubMemberRepository.countByClubId(clubId)).willReturn(0L);
        given(clubPreMemberRepository.countByClubId(clubId)).willReturn(0L);

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
        assertThat(response.syncedMemberCount()).isEqualTo(0);
        assertThat(response.sheetUrl())
            .isEqualTo("https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit");
    }

    @Test
    @DisplayName("updateSheetId는 null memberListMapping 분석 결과를 등록 서비스로 전달한다")
    void updateSheetIdDelegatesNullMemberListMapping() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetUrl = "https://docs.google.com/spreadsheets/d/test-sheet-id/edit";
        ClubSheetIdUpdateRequest request = new ClubSheetIdUpdateRequest(spreadsheetUrl);
        SheetHeaderMapper.SheetAnalysisResult analysisResult = new SheetHeaderMapper.SheetAnalysisResult(
            null,
            null,
            null
        );

        given(clubRepository.existsById(clubId)).willReturn(true);
        given(sheetHeaderMapper.analyzeAllSheets("test-sheet-id")).willReturn(analysisResult);

        // when
        clubMemberSheetService.updateSheetId(clubId, requesterId, request);

        // then
        verify(clubSheetRegistrationService).updateSheetRegistration(
            eq(clubId),
            eq("test-sheet-id"),
            eq(analysisResult)
        );
    }
}
