package gg.agit.konect.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_SHEET_ID;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberSheetService {

    private static final String SHEET_RANGE = "A1";
    private static final String CLEAR_RANGE = "A:F";
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<Object> HEADER_ROW =
        List.of("Name", "StudentId", "Email", "Phone", "Position", "JoinedAt");

    private final Sheets googleSheetsService;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPermissionValidator clubPermissionValidator;

    @Transactional
    public void updateSheetId(
        Integer clubId,
        Integer requesterId,
        ClubSheetIdUpdateRequest request
    ) {
        Club club = clubRepository.getById(clubId);
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);
        club.updateGoogleSheetId(request.spreadsheetId());
    }

    public ClubMemberSheetSyncResponse syncMembersToSheet(
        Integer clubId,
        Integer requesterId
    ) {
        Club club = clubRepository.getById(clubId);
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);

        String spreadsheetId = club.getGoogleSheetId();
        if (spreadsheetId == null || spreadsheetId.isBlank()) {
            throw CustomException.of(NOT_FOUND_CLUB_SHEET_ID);
        }

        List<ClubMember> members = clubMemberRepository.findAllByClubId(clubId);

        try {
            clearSheet(spreadsheetId);
            writeSheet(spreadsheetId, buildRows(members));
        } catch (IOException e) {
            log.error(
                "Google Sheets sync failed. spreadsheetId={}, cause={}",
                spreadsheetId, e.getMessage(), e
            );
            throw CustomException.of(FAILED_SYNC_GOOGLE_SHEET);
        }

        return ClubMemberSheetSyncResponse.of(members.size(), spreadsheetId);
    }

    private void clearSheet(String spreadsheetId) throws IOException {
        googleSheetsService.spreadsheets().values()
            .clear(spreadsheetId, CLEAR_RANGE, new ClearValuesRequest())
            .execute();
    }

    private void writeSheet(String spreadsheetId, List<List<Object>> rows) throws IOException {
        ValueRange body = new ValueRange().setValues(rows);
        googleSheetsService.spreadsheets().values()
            .update(spreadsheetId, SHEET_RANGE, body)
            .setValueInputOption("USER_ENTERED")
            .execute();
    }

    private List<List<Object>> buildRows(List<ClubMember> members) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(HEADER_ROW);

        for (ClubMember member : members) {
            rows.add(List.of(
                member.getUser().getName(),
                member.getUser().getStudentNumber(),
                member.getUser().getEmail(),
                member.getUser().getPhoneNumber() != null
                    ? "'" + member.getUser().getPhoneNumber() : "",
                member.getClubPosition().getDescription(),
                member.getCreatedAt().format(DATE_FORMATTER)
            ));
        }

        return rows;
    }
}
