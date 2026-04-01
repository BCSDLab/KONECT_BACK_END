package gg.agit.konect.domain.club.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static gg.agit.konect.domain.club.service.GoogleApiTestUtils.googleException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.event.SheetSyncFailedEvent;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

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
    private Sheets.Spreadsheets.Values.Update updateRequest;

    @Mock
    private Sheets.Spreadsheets.BatchUpdate batchUpdateRequest;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubPreMemberRepository clubPreMemberRepository;

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
        given(clubPreMemberRepository.findAllByClubId(clubId)).willReturn(List.of());
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

    @Test
    @DisplayName("시트 동기화 시 사전 회원도 함께 덮어쓴다")
    void executeWithSortWritesClubMembersAndPreMembers() throws Exception {
        // given
        Integer clubId = 1;
        String spreadsheetId = "spreadsheet-id";
        Club club = ClubFixture.create(UniversityFixture.create());
        club.updateGoogleSheetId(spreadsheetId);

        User memberUser = UserFixture.createUser(club.getUniversity(), "김회원", "2021000001");
        ClubMember member = ClubMemberFixture.createMember(club, memberUser);
        setCreatedAt(member, LocalDateTime.of(2024, 3, 1, 10, 0));

        ClubPreMember preMember = ClubPreMember.builder()
            .club(club)
            .studentNumber("2024000001")
            .name("박사전")
            .clubPosition(ClubPosition.MEMBER)
            .build();
        setCreatedAt(preMember, LocalDateTime.of(2024, 3, 2, 10, 0));

        given(clubRepository.getById(clubId)).willReturn(club);
        given(clubMemberRepository.findAllByClubId(clubId)).willReturn(List.of(member));
        given(clubPreMemberRepository.findAllByClubId(clubId)).willReturn(List.of(preMember));
        given(googleSheetsService.spreadsheets()).willReturn(spreadsheets);
        given(spreadsheets.values()).willReturn(values);
        given(values.clear(eq(spreadsheetId), eq("A:F"), any(ClearValuesRequest.class)))
            .willReturn(clearRequest);
        given(values.update(eq(spreadsheetId), eq("A1"), any(ValueRange.class)))
            .willReturn(updateRequest);
        given(updateRequest.setValueInputOption("USER_ENTERED")).willReturn(updateRequest);
        given(updateRequest.execute()).willReturn(new UpdateValuesResponse());
        given(spreadsheets.batchUpdate(eq(spreadsheetId), any())).willReturn(batchUpdateRequest);
        given(batchUpdateRequest.execute()).willReturn(new BatchUpdateSpreadsheetResponse());

        // when
        sheetSyncExecutor.executeWithSort(clubId, ClubSheetSortKey.NAME, true);

        // then
        verify(values).update(eq(spreadsheetId), eq("A1"), argThat((ValueRange body) ->
            body.getValues().equals(List.of(
                List.of("Name", "StudentId", "Email", "Phone", "Position", "JoinedAt"),
                List.of("김회원", "2021000001", "2021000001@koreatech.ac.kr", "", "일반회원", "2024-03-01"),
                List.of("박사전", "2024000001", "", "", "일반회원", "2024-03-02")
            ))
        ));
    }

    private void setCreatedAt(Object target, LocalDateTime createdAt) throws Exception {
        Field createdAtField = target.getClass().getSuperclass().getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(target, createdAt);
    }
}
