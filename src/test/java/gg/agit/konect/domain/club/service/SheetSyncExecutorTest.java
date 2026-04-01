package gg.agit.konect.domain.club.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static gg.agit.konect.domain.club.service.GoogleApiTestUtils.googleException;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;

import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.event.SheetSyncFailedEvent;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;

class SheetSyncExecutorTest extends ServiceTestSupport {

    @Mock
    private Sheets googleSheetsService;

    @Mock
    private Sheets.Spreadsheets spreadsheets;

    @Mock
    private Sheets.Spreadsheets.Values values;

    @Mock
    private Sheets.Spreadsheets.Values.Clear clearRequest;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SheetSyncExecutor sheetSyncExecutor;

    @Test
    @DisplayName("시트 동기화 권한 오류는 실패 이벤트로 발행한다")
    void executeWithSortPublishesFailureEventWhenAccessDenied() throws Exception {
        // given
        Integer clubId = 1;
        String spreadsheetId = "spreadsheet-id";
        Club club = ClubFixture.create(UniversityFixture.create());
        club.updateGoogleSheetId(spreadsheetId);

        given(clubRepository.getById(clubId)).willReturn(club);
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of());
        given(googleSheetsService.spreadsheets()).willReturn(spreadsheets);
        given(spreadsheets.values()).willReturn(values);
        given(values.clear(eq(spreadsheetId), eq("A:F"), any(ClearValuesRequest.class)))
            .willReturn(clearRequest);
        given(clearRequest.execute()).willThrow(googleException(403, "accessDenied"));

        // when
        sheetSyncExecutor.executeWithSort(clubId, ClubSheetSortKey.NAME, true);

        // then
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
            event instanceof SheetSyncFailedEvent sheetSyncFailedEvent
                && sheetSyncFailedEvent.clubId().equals(clubId)
                && sheetSyncFailedEvent.spreadsheetId().equals(spreadsheetId)
                && sheetSyncFailedEvent.accessDenied()
        ));
    }
}
