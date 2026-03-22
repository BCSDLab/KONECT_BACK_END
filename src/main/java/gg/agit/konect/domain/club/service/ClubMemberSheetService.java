package gg.agit.konect.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_SHEET_ID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMemberSheetService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPermissionValidator clubPermissionValidator;
    private final SheetSyncExecutor sheetSyncExecutor;
    private final SheetHeaderMapper sheetHeaderMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void updateSheetId(
        Integer clubId,
        Integer requesterId,
        ClubSheetIdUpdateRequest request
    ) {
        String spreadsheetId = SpreadsheetUrlParser.extractId(request.spreadsheetUrl());

        SheetHeaderMapper.SheetAnalysisResult result =
            sheetHeaderMapper.analyzeAllSheets(spreadsheetId);
        String mappingJson = null;
        try {
            mappingJson = objectMapper.writeValueAsString(result.memberListMapping().toMap());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize mapping, skipping. cause={}", e.getMessage());
        }

        Club club = clubRepository.getById(clubId);
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);
        club.updateGoogleSheetId(spreadsheetId);
        if (mappingJson != null) {
            club.updateSheetColumnMapping(mappingJson);
        }
    }

    @Transactional(readOnly = true)
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
        sheetSyncExecutor.executeWithSort(clubId, sortKey, ascending);

        return ClubMemberSheetSyncResponse.of((int)memberCount, spreadsheetId);
    }
}
