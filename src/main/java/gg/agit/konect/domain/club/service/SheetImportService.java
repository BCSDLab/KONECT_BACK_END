package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetImportService {

    private final Sheets googleSheetsService;
    private final SheetHeaderMapper sheetHeaderMapper;
    private final ClubRepository clubRepository;
    private final ClubPreMemberRepository clubPreMemberRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPermissionValidator clubPermissionValidator;

    @Transactional
    public int importPreMembersFromSheet(
        Integer clubId,
        Integer requesterId,
        String spreadsheetUrl
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);
        Club club = clubRepository.getById(clubId);

        String spreadsheetId = SpreadsheetUrlParser.extractId(spreadsheetUrl);

        SheetHeaderMapper.SheetAnalysisResult analysis =
            sheetHeaderMapper.analyzeAllSheets(spreadsheetId);
        SheetColumnMapping mapping = analysis.memberListMapping();

        List<List<Object>> rows = readDataRows(spreadsheetId, mapping);

        // N+1 방지: 루프 전에 기존 부원/사전 회원 학번 Set을 한 번만 조회
        Set<String> existingMemberStudentNumbers =
            clubMemberRepository.findStudentNumbersByClubId(clubId);
        Set<String> existingPreMemberKeys =
            clubPreMemberRepository.findStudentNumberNameKeysByClubId(clubId);

        int imported = 0;

        for (List<Object> row : rows) {
            String name = getCell(row, mapping, SheetColumnMapping.NAME);
            String studentNumber = getCell(row, mapping, SheetColumnMapping.STUDENT_ID);

            if (name.isBlank() || studentNumber.isBlank()) {
                continue;
            }

            if (existingPreMemberKeys.contains(studentNumber + "|" + name)) {
                continue;
            }

            if (existingMemberStudentNumbers.contains(studentNumber)) {
                continue;
            }

            String positionStr = getCell(row, mapping, SheetColumnMapping.POSITION);
            ClubPosition position = resolvePosition(positionStr);

            ClubPreMember preMember = ClubPreMember.builder()
                .club(club)
                .studentNumber(studentNumber)
                .name(name)
                .clubPosition(position)
                .build();

            clubPreMemberRepository.save(preMember);
            imported++;
        }

        log.info(
            "Sheet import done. clubId={}, spreadsheetId={}, imported={}",
            clubId, spreadsheetId, imported
        );
        return imported;
    }

    private List<List<Object>> readDataRows(String spreadsheetId, SheetColumnMapping mapping) {
        try {
            int dataStartRow = mapping.getDataStartRow();
            String range = "A" + dataStartRow + ":Z";
            ValueRange response = googleSheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

            List<List<Object>> values = response.getValues();
            return values != null ? values : List.of();

        } catch (IOException e) {
            log.error("Failed to read sheet data. spreadsheetId={}", spreadsheetId, e);
            return List.of();
        }
    }

    private String getCell(List<Object> row, SheetColumnMapping mapping, String field) {
        int col = mapping.getColumnIndex(field);
        if (col < 0 || col >= row.size()) {
            return "";
        }
        String value = row.get(col).toString().trim();
        if (value.startsWith("'")) {
            return value.substring(1);
        }
        return value;
    }

    private ClubPosition resolvePosition(String positionStr) {
        for (ClubPosition pos : ClubPosition.values()) {
            if (pos.getDescription().equals(positionStr)
                || pos.name().equalsIgnoreCase(positionStr)) {
                return pos;
            }
        }
        return ClubPosition.MEMBER;
    }
}
