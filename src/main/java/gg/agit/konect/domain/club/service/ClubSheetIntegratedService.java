package gg.agit.konect.domain.club.service;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.club.dto.SheetImportResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClubSheetIntegratedService {

    private final ClubPermissionValidator clubPermissionValidator;
    private final GoogleSheetPermissionService googleSheetPermissionService;
    private final SheetHeaderMapper sheetHeaderMapper;
    private final ClubMemberSheetService clubMemberSheetService;
    private final SheetImportService sheetImportService;

    public SheetImportResponse analyzeAndImportPreMembers(
        Integer clubId,
        Integer requesterId,
        String spreadsheetUrl
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);

        String spreadsheetId = SpreadsheetUrlParser.extractId(spreadsheetUrl);
        // OAuth 미연결이면 기존 동작대로 검증을 건너뛴다.
        // 다만 Drive OAuth가 연결된 경우에는 요청자 계정의 실제 시트 접근 권한을 먼저 검증한다.
        googleSheetPermissionService.validateRequesterAccessAndTryGrantServiceAccountWriterAccess(
            requesterId,
            spreadsheetId
        );

        SheetHeaderMapper.SheetAnalysisResult analysis =
            sheetHeaderMapper.analyzeAllSheets(spreadsheetId);

        clubMemberSheetService.updateSheetId(
            clubId,
            requesterId,
            spreadsheetId,
            analysis
        );
        return sheetImportService.importPreMembersFromSheet(
            clubId,
            requesterId,
            spreadsheetId,
            analysis.memberListMapping()
        );
    }
}
