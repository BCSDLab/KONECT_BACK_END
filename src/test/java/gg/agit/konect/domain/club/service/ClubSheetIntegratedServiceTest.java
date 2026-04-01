package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.dto.SheetImportResponse;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;

class ClubSheetIntegratedServiceTest extends ServiceTestSupport {

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @Mock
    private SheetHeaderMapper sheetHeaderMapper;

    @Mock
    private GoogleSheetPermissionService googleSheetPermissionService;

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
        String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms";
        SheetHeaderMapper.SheetAnalysisResult analysis =
            new SheetHeaderMapper.SheetAnalysisResult(SheetColumnMapping.defaultMapping(), null, null);
        SheetImportResponse expected = SheetImportResponse.of(3, 1, List.of("warn"));

        given(sheetHeaderMapper.analyzeAllSheets(spreadsheetId)).willReturn(analysis);
        given(sheetImportService.importPreMembersFromSheet(
            clubId,
            requesterId,
            spreadsheetId,
            analysis.memberListMapping()
        ))
            .willReturn(expected);

        // when
        SheetImportResponse actual = clubSheetIntegratedService.analyzeAndImportPreMembers(
            clubId,
            requesterId,
            spreadsheetUrl
        );

        // then
        InOrder inOrder = inOrder(
            clubPermissionValidator,
            googleSheetPermissionService,
            sheetHeaderMapper,
            clubMemberSheetService,
            sheetImportService
        );
        inOrder.verify(clubPermissionValidator).validateManagerAccess(clubId, requesterId);
        inOrder.verify(googleSheetPermissionService)
            .validateRequesterAccessAndTryGrantServiceAccountWriterAccess(requesterId, spreadsheetId);
        inOrder.verify(sheetHeaderMapper).analyzeAllSheets(spreadsheetId);
        inOrder.verify(clubMemberSheetService).updateSheetId(
            clubId,
            requesterId,
            spreadsheetId,
            analysis
        );
        inOrder.verify(sheetImportService).importPreMembersFromSheet(
            clubId,
            requesterId,
            spreadsheetId,
            analysis.memberListMapping()
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("구글 시트 권한 검증 후에도 기존 흐름대로 사전 회원 가져오기를 수행한다")
    void analyzeAndImportPreMembersContinuesAfterPermissionValidation() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetUrl =
            "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit";
        String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms";
        SheetHeaderMapper.SheetAnalysisResult analysis =
            new SheetHeaderMapper.SheetAnalysisResult(SheetColumnMapping.defaultMapping(), null, null);
        SheetImportResponse expected = SheetImportResponse.of(1, 0, List.of());

        given(sheetHeaderMapper.analyzeAllSheets(spreadsheetId)).willReturn(analysis);
        given(sheetImportService.importPreMembersFromSheet(
            clubId,
            requesterId,
            spreadsheetId,
            analysis.memberListMapping()
        ))
            .willReturn(expected);

        // when
        SheetImportResponse actual = clubSheetIntegratedService.analyzeAndImportPreMembers(
            clubId,
            requesterId,
            spreadsheetUrl
        );

        // then
        InOrder inOrder = inOrder(
            clubPermissionValidator,
            googleSheetPermissionService,
            sheetHeaderMapper,
            clubMemberSheetService,
            sheetImportService
        );
        inOrder.verify(clubPermissionValidator).validateManagerAccess(clubId, requesterId);
        inOrder.verify(googleSheetPermissionService)
            .validateRequesterAccessAndTryGrantServiceAccountWriterAccess(requesterId, spreadsheetId);
        inOrder.verify(sheetHeaderMapper).analyzeAllSheets(spreadsheetId);
        inOrder.verify(clubMemberSheetService).updateSheetId(
            clubId,
            requesterId,
            spreadsheetId,
            analysis
        );
        inOrder.verify(sheetImportService).importPreMembersFromSheet(
            clubId,
            requesterId,
            spreadsheetId,
            analysis.memberListMapping()
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("자동 권한 부여 중 예외가 발생하면 후속 시트 작업을 진행하지 않는다")
    void analyzeAndImportPreMembersStopsWhenAutoGrantThrowsException() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetUrl =
            "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit";
        String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms";
        CustomException expected = CustomException.of(ApiResponseCode.FAILED_INIT_GOOGLE_DRIVE);

        willThrow(expected).given(googleSheetPermissionService)
            .validateRequesterAccessAndTryGrantServiceAccountWriterAccess(requesterId, spreadsheetId);

        // when & then
        assertThatThrownBy(() -> clubSheetIntegratedService.analyzeAndImportPreMembers(
            clubId,
            requesterId,
            spreadsheetUrl
        ))
            .isSameAs(expected);
        verifyNoInteractions(sheetHeaderMapper, clubMemberSheetService, sheetImportService);
    }

    @Test
    @DisplayName("요청자 계정이 시트 접근 권한이 없으면 후속 시트 작업을 진행하지 않는다")
    void analyzeAndImportPreMembersStopsWhenRequesterHasNoSpreadsheetAccess() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetUrl =
            "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit";
        String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms";
        CustomException expected = CustomException.of(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS);

        willThrow(expected).given(googleSheetPermissionService)
            .validateRequesterAccessAndTryGrantServiceAccountWriterAccess(requesterId, spreadsheetId);

        // when & then
        assertThatThrownBy(() -> clubSheetIntegratedService.analyzeAndImportPreMembers(
            clubId,
            requesterId,
            spreadsheetUrl
        ))
            .isSameAs(expected);
        verifyNoInteractions(sheetHeaderMapper, clubMemberSheetService, sheetImportService);
    }
}
