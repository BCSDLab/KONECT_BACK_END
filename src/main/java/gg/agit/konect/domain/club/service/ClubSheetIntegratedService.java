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
        // Best-effort: OAuth 미연결/권한 부여 실패여도 이미 수동 공유된 시트는 그대로 읽을 수 있다.
        googleSheetPermissionService.tryGrantServiceAccountWriterAccess(requesterId, spreadsheetId);

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
