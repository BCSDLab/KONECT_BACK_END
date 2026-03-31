package gg.agit.konect.domain.club.service;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.club.dto.SheetImportResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClubSheetIntegratedService {

    private final ClubPermissionValidator clubPermissionValidator;
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
