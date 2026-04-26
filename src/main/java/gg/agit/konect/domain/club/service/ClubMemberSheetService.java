package gg.agit.konect.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_SHEET_ID;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClubMemberSheetService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPreMemberRepository clubPreMemberRepository;
    private final ClubPermissionValidator clubPermissionValidator;
    private final SheetSyncExecutor sheetSyncExecutor;
    private final SheetHeaderMapper sheetHeaderMapper;
    private final ClubSheetRegistrationService clubSheetRegistrationService;

    public void updateSheetId(
        Integer clubId,
        Integer requesterId,
        ClubSheetIdUpdateRequest request
    ) {
        String spreadsheetId = SpreadsheetUrlParser.extractId(request.spreadsheetUrl());
        validateClubExists(clubId);
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);

        SheetHeaderMapper.SheetAnalysisResult result =
            sheetHeaderMapper.analyzeAllSheets(spreadsheetId);
        clubSheetRegistrationService.updateSheetRegistration(clubId, spreadsheetId, result);
    }

    void updateSheetId(
        Integer clubId,
        Integer requesterId,
        String spreadsheetId,
        SheetHeaderMapper.SheetAnalysisResult result
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);
        clubSheetRegistrationService.updateSheetRegistration(clubId, spreadsheetId, result);
    }

    private void validateClubExists(Integer clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw CustomException.of(ApiResponseCode.NOT_FOUND_CLUB);
        }
    }

    public ClubMemberSheetSyncResponse syncMembersToSheet(
        Integer clubId,
        Integer requesterId,
        ClubSheetSortKey sortKey,
        boolean ascending
    ) {
        Club club = clubRepository.getById(clubId);
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);

        String spreadsheetId = club.getGoogleSheetId();
        if (spreadsheetId == null || spreadsheetId.isBlank()) {
            throw CustomException.of(NOT_FOUND_CLUB_SHEET_ID);
        }

        long memberCount = clubMemberRepository.countByClubId(clubId);
        long preMemberCount = clubPreMemberRepository.countByClubId(clubId);
        sheetSyncExecutor.executeWithSort(clubId, sortKey, ascending);

        return ClubMemberSheetSyncResponse.of(Math.toIntExact(memberCount + preMemberCount), spreadsheetId);
    }
}
