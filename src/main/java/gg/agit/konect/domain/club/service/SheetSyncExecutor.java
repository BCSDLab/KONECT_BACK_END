package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BasicFilter;
import com.google.api.services.sheets.v4.model.BatchClearValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SetBasicFilterRequest;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.club.event.SheetSyncFailedEvent;
import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.global.util.PhoneNumberNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SheetSyncExecutor {

    private static final String SHEET_RANGE = "A1";
    private static final int ALPHABET_SIZE = 26;
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<Object> HEADER_ROW = List.of(
        "Name", "StudentId", "Email", "Phone", "Position", "JoinedAt"
    );

    private final Sheets googleSheetsService;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPreMemberRepository clubPreMemberRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Async("sheetSyncTaskExecutor")
    @Transactional(readOnly = true)
    public void executeWithSort(Integer clubId, ClubSheetSortKey sortKey, boolean ascending) {
        Club club = clubRepository.getById(clubId);
        String spreadsheetId = club.getGoogleSheetId();
        if (spreadsheetId == null || spreadsheetId.isBlank()) {
            return;
        }

        SheetColumnMapping mapping = resolveMapping(club);
        List<ClubMember> members = clubMemberRepository.findAllByClubId(clubId);
        List<ClubPreMember> preMembers = clubPreMemberRepository.findAllByClubId(clubId);
        List<SheetSyncRow> sorted = sort(toSheetSyncRows(members, preMembers), sortKey, ascending);

        try {
            if (club.getSheetColumnMapping() != null) {
                updateMappedColumns(spreadsheetId, sorted, mapping);
            } else {
                clearAndWriteAll(spreadsheetId, sorted);
                applyFormat(spreadsheetId);
            }
            log.info(
                "Sheet sync done. clubId={}, members={}, preMembers={}",
                clubId,
                members.size(),
                preMembers.size()
            );
        } catch (IOException e) {
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)) {
                log.warn(
                    "Google Sheets access denied during sheet sync. clubId={}, spreadsheetId={}, cause={}",
                    clubId,
                    spreadsheetId,
                    e.getMessage()
                );
                applicationEventPublisher.publishEvent(
                    SheetSyncFailedEvent.accessDenied(clubId, spreadsheetId, e.getMessage())
                );
                return;
            }
            log.error(
                "Sheet sync failed. clubId={}, spreadsheetId={}, cause={}",
                clubId, spreadsheetId, e.getMessage(), e
            );
            applicationEventPublisher.publishEvent(
                SheetSyncFailedEvent.unexpected(clubId, spreadsheetId, e.getMessage())
            );
        }
    }

    private SheetColumnMapping resolveRawMapping(String mappingJson) {
        try {
            Map<String, Object> raw = objectMapper.readValue(
                mappingJson, new TypeReference<>() {}
            );
            int dataStartRow = raw.containsKey("dataStartRow")
                ? ((Number)raw.get("dataStartRow")).intValue() : 2;
            Map<String, Integer> fieldMap = new HashMap<>();
            raw.forEach((key, value) -> {
                if (!"dataStartRow".equals(key) && value instanceof Number num) {
                    fieldMap.put(key, num.intValue());
                }
            });
            return new SheetColumnMapping(fieldMap, dataStartRow);
        } catch (Exception e) {
            log.warn("Failed to parse raw mapping, using default. cause={}", e.getMessage());
            return SheetColumnMapping.defaultMapping();
        }
    }

    private SheetColumnMapping resolveMapping(Club club) {
        String mappingJson = club.getSheetColumnMapping();
        if (mappingJson == null || mappingJson.isBlank()) {
            return SheetColumnMapping.defaultMapping();
        }
        return resolveRawMapping(mappingJson);
    }

    private void updateMappedColumns(
        String spreadsheetId,
        List<SheetSyncRow> members,
        SheetColumnMapping mapping
    ) throws IOException {
        int dataStartRow = mapping.getDataStartRow();
        clearMappedColumns(spreadsheetId, mapping, dataStartRow);
        Map<Integer, List<Object>> columnData = buildColumnData(members, mapping);

        List<ValueRange> data = new ArrayList<>();
        for (Map.Entry<Integer, List<Object>> entry : columnData.entrySet()) {
            int colIndex = entry.getKey();
            String colLetter = columnLetter(colIndex);
            String range = colLetter + dataStartRow + ":" + colLetter;
            List<List<Object>> wrapped =
                entry.getValue().stream().map(v -> List.of((Object)v)).toList();
            data.add(new ValueRange().setRange(range).setValues(wrapped));
        }

        if (!data.isEmpty()) {
            googleSheetsService.spreadsheets().values()
                .batchUpdate(spreadsheetId,
                    new BatchUpdateValuesRequest()
                        .setValueInputOption("USER_ENTERED")
                        .setData(data))
                .execute();
        }
    }

    private void clearMappedColumns(
        String spreadsheetId,
        SheetColumnMapping mapping,
        int dataStartRow
    ) throws IOException {
        List<String> clearRanges = new ArrayList<>();
        for (String field : List.of(
            SheetColumnMapping.NAME, SheetColumnMapping.STUDENT_ID, SheetColumnMapping.EMAIL,
            SheetColumnMapping.PHONE, SheetColumnMapping.POSITION, SheetColumnMapping.JOINED_AT
        )) {
            int colIndex = mapping.getColumnIndex(field);
            if (colIndex >= 0) {
                String colLetter = columnLetter(colIndex);
                clearRanges.add(colLetter + dataStartRow + ":" + colLetter);
            }
        }
        if (!clearRanges.isEmpty()) {
            googleSheetsService.spreadsheets().values()
                .batchClear(spreadsheetId, new BatchClearValuesRequest().setRanges(clearRanges))
                .execute();
        }
    }

    private Map<Integer, List<Object>> buildColumnData(
        List<SheetSyncRow> members,
        SheetColumnMapping mapping
    ) {
        Map<Integer, List<Object>> columns = new HashMap<>();

        for (SheetSyncRow member : members) {
            putValue(columns, mapping, SheetColumnMapping.NAME,
                member.name());
            putValue(columns, mapping, SheetColumnMapping.STUDENT_ID,
                member.studentNumber());
            putValue(columns, mapping, SheetColumnMapping.EMAIL,
                member.email());
            putValue(columns, mapping, SheetColumnMapping.PHONE,
                member.phone());
            putValue(columns, mapping, SheetColumnMapping.POSITION,
                member.positionDescription());
            putValue(columns, mapping, SheetColumnMapping.JOINED_AT,
                member.joinedAt());
        }

        return columns;
    }

    private void putValue(
        Map<Integer, List<Object>> columns,
        SheetColumnMapping mapping,
        String field,
        Object value
    ) {
        int colIndex = mapping.getColumnIndex(field);
        if (colIndex >= 0) {
            columns.computeIfAbsent(colIndex, k -> new ArrayList<>()).add(value != null ? value : "");
        }
    }

    private void clearAndWriteAll(
        String spreadsheetId,
        List<SheetSyncRow> members
    ) throws IOException {
        String clearRange = "A:F";
        googleSheetsService.spreadsheets().values()
            .clear(spreadsheetId, clearRange, new ClearValuesRequest())
            .execute();

        List<List<Object>> rows = new ArrayList<>();
        rows.add(HEADER_ROW);

        for (SheetSyncRow member : members) {
            rows.add(List.of(
                member.name(),
                member.studentNumber(),
                member.email(),
                member.phone(),
                member.positionDescription(),
                member.joinedAt()
            ));
        }

        ValueRange body = new ValueRange().setValues(rows);
        googleSheetsService.spreadsheets().values()
            .update(spreadsheetId, SHEET_RANGE, body)
            .setValueInputOption("USER_ENTERED")
            .execute();
    }

    private void applyFormat(String spreadsheetId) throws IOException {
        List<Request> requests = new ArrayList<>();

        requests.add(new Request().setUpdateSheetProperties(
            new UpdateSheetPropertiesRequest()
                .setProperties(new SheetProperties()
                    .setGridProperties(new GridProperties().setFrozenRowCount(1)))
                .setFields("gridProperties.frozenRowCount")
        ));

        requests.add(new Request().setSetBasicFilter(
            new SetBasicFilterRequest()
                .setFilter(new BasicFilter()
                    .setRange(new GridRange().setSheetId(0)))
        ));

        googleSheetsService.spreadsheets()
            .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests))
            .execute();
    }

    private List<SheetSyncRow> sort(
        List<SheetSyncRow> members,
        ClubSheetSortKey sortKey,
        boolean ascending
    ) {
        Comparator<SheetSyncRow> comparator = switch (sortKey) {
            case NAME -> Comparator.comparing(SheetSyncRow::name);
            case STUDENT_ID -> Comparator.comparing(SheetSyncRow::studentNumber);
            case POSITION -> Comparator.comparingInt(SheetSyncRow::positionPriority);
            case JOINED_AT -> Comparator.comparing(SheetSyncRow::joinedAtRaw);

        };

        if (!ascending) {
            comparator = comparator.reversed();
        }

        return members.stream().sorted(comparator).toList();
    }

    private List<SheetSyncRow> toSheetSyncRows(
        List<ClubMember> members,
        List<ClubPreMember> preMembers
    ) {
        List<SheetSyncRow> rows = new ArrayList<>(members.size() + preMembers.size());
        for (ClubMember member : members) {
            rows.add(SheetSyncRow.from(member));
        }
        for (ClubPreMember preMember : preMembers) {
            rows.add(SheetSyncRow.from(preMember));
        }
        return rows;
    }

    private String columnLetter(int index) {
        StringBuilder sb = new StringBuilder();
        index++;
        while (index > 0) {
            index--;
            sb.insert(0, (char)('A' + index % ALPHABET_SIZE));
            index /= ALPHABET_SIZE;
        }
        return sb.toString();
    }

    private record SheetSyncRow(
        String name,
        String studentNumber,
        String email,
        String phone,
        String positionDescription,
        int positionPriority,
        String joinedAt,
        java.time.LocalDateTime joinedAtRaw
    ) {
        private static SheetSyncRow from(ClubMember member) {
            String phone = PhoneNumberNormalizer.format(member.getUser().getPhoneNumber());
            return new SheetSyncRow(
                member.getUser().getName(),
                member.getUser().getStudentNumber(),
                member.getUser().getEmail(),
                phone != null ? phone : "",
                member.getClubPosition().getDescription(),
                member.getClubPosition().getPriority(),
                member.getCreatedAt().format(DATE_FORMATTER),
                member.getCreatedAt()
            );
        }

        private static SheetSyncRow from(ClubPreMember preMember) {
            return new SheetSyncRow(
                preMember.getName(),
                preMember.getStudentNumber(),
                "",
                "",
                preMember.getClubPosition().getDescription(),
                preMember.getClubPosition().getPriority(),
                preMember.getCreatedAt().format(DATE_FORMATTER),
                preMember.getCreatedAt()
            );
        }
    }
}
