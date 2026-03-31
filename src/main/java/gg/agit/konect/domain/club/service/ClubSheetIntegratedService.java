package gg.agit.konect.domain.club.service;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.domain.club.dto.SheetImportResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClubSheetIntegratedService {

    private final ClubMemberSheetService clubMemberSheetService;
    private final SheetImportService sheetImportService;

    public SheetImportResponse analyzeAndImportPreMembers(
        Integer clubId,
        Integer requesterId,
        String spreadsheetUrl
    ) {
        clubMemberSheetService.updateSheetId(
            clubId,
            requesterId,
            new ClubSheetIdUpdateRequest(spreadsheetUrl)
        );
        return sheetImportService.importPreMembersFromSheet(clubId, requesterId, spreadsheetUrl);
    }
}
