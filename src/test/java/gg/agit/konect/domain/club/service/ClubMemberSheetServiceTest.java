package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
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
}
