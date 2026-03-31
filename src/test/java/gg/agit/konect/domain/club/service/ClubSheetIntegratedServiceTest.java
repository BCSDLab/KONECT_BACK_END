package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.dto.SheetImportResponse;
import gg.agit.konect.support.ServiceTestSupport;

class ClubSheetIntegratedServiceTest extends ServiceTestSupport {

    @Mock
    private ClubMemberSheetService clubMemberSheetService;

    @Mock
    private SheetImportService sheetImportService;

    @InjectMocks
    private ClubSheetIntegratedService clubSheetIntegratedService;

    @Test
    @DisplayName("시트 분석 등록 후 사전 회원 가져오기를 순서대로 실행한다")
    void analyzeAndImportPreMembersSuccess() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetUrl =
            "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit";
        SheetImportResponse expected = SheetImportResponse.of(3, 1, java.util.List.of("warn"));

        given(sheetImportService.importPreMembersFromSheet(clubId, requesterId, spreadsheetUrl))
            .willReturn(expected);

        // when
        SheetImportResponse actual = clubSheetIntegratedService.analyzeAndImportPreMembers(
            clubId,
            requesterId,
            spreadsheetUrl
        );

        // then
        InOrder inOrder = inOrder(clubMemberSheetService, sheetImportService);
        inOrder.verify(clubMemberSheetService).updateSheetId(
            clubId,
            requesterId,
            new gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest(spreadsheetUrl)
        );
        inOrder.verify(sheetImportService).importPreMembersFromSheet(
            clubId,
            requesterId,
            spreadsheetUrl
        );
        assertThat(actual).isEqualTo(expected);
    }
}
