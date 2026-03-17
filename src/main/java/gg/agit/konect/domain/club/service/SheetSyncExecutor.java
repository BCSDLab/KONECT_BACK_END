package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BasicFilter;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SetBasicFilterRequest;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubFeePayment;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubFeePaymentRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
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

    private final Sheets googleSheetsService;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubFeePaymentRepository clubFeePaymentRepository;
    private final ObjectMapper objectMapper;

    @Async("sheetSyncTaskExecutor")
    @Transactional(readOnly = true)
    public void execute(Integer clubId) {
        executeWithSort(clubId, ClubSheetSortKey.POSITION, true);
    }

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
        List<ClubFeePayment> payments = clubFeePaymentRepository.findAllByClubId(clubId);

        Map<Integer, ClubFeePayment> paymentMap = payments.stream()
            .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        List<ClubMember> sorted = sort(members, paymentMap, sortKey, ascending);

        try {
            if (club.getSheetColumnMapping() != null) {
                updateMappedColumns(spreadsheetId, sorted, paymentMap, mapping);
            } else {
                clearAndWriteAll(spreadsheetId, sorted, paymentMap);
                applyFormat(spreadsheetId);
            }
        } catch (IOException e) {
            log.error(
                "Sheet sync failed. clubId={}, spreadsheetId={}, cause={}",
                clubId, spreadsheetId, e.getMessage(), e
            );
        }

        log.info("Sheet sync done. clubId={}, members={}", clubId, members.size());
    }

    private SheetColumnMapping resolveMapping(Club club) {
        String mappingJson = club.getSheetColumnMapping();
        if (mappingJson == null || mappingJson.isBlank()) {
            return SheetColumnMapping.defaultMapping();
        }
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
            log.warn("Failed to parse sheet mapping, using default. cause={}", e.getMessage());
            return SheetColumnMapping.defaultMapping();
        }
    }

    private void updateMappedColumns(
        String spreadsheetId,
        List<ClubMember> members,
        Map<Integer, ClubFeePayment> paymentMap,
        SheetColumnMapping mapping
    ) throws IOException {
        int dataStartRow = mapping.getDataStartRow();
        Map<Integer, List<Object>> columnData = buildColumnData(members, paymentMap, mapping);

        List<ValueRange> data = new ArrayList<>();
        for (Map.Entry<Integer, List<Object>> entry : columnData.entrySet()) {
            int colIndex = entry.getKey();
            List<Object> values = entry.getValue();
            String colLetter = columnLetter(colIndex);
            String range = colLetter + dataStartRow + ":" + colLetter;
            List<List<Object>> wrapped = values.stream().map(v -> List.of((Object)v)).toList();
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

    private Map<Integer, List<Object>> buildColumnData(
        List<ClubMember> members,
        Map<Integer, ClubFeePayment> paymentMap,
        SheetColumnMapping mapping
    ) {
        Map<Integer, List<Object>> columns = new HashMap<>();

        for (ClubMember member : members) {
            Integer userId = member.getUser().getId();
            ClubFeePayment payment = paymentMap.get(userId);

            putValue(columns, mapping, SheetColumnMapping.NAME,
                member.getUser().getName());
            putValue(columns, mapping, SheetColumnMapping.STUDENT_ID,
                member.getUser().getStudentNumber());
            putValue(columns, mapping, SheetColumnMapping.EMAIL,
                member.getUser().getEmail());
            putValue(columns, mapping, SheetColumnMapping.PHONE,
                member.getUser().getPhoneNumber() != null
                    ? "'" + member.getUser().getPhoneNumber() : "");
            putValue(columns, mapping, SheetColumnMapping.POSITION,
                member.getClubPosition().getDescription());
            putValue(columns, mapping, SheetColumnMapping.JOINED_AT,
                member.getCreatedAt().format(DATE_FORMATTER));
            putValue(columns, mapping, SheetColumnMapping.FEE_PAID,
                payment != null && payment.isPaid() ? "Y" : "N");
            putValue(columns, mapping, SheetColumnMapping.PAID_AT,
                payment != null && payment.getApprovedAt() != null
                    ? payment.getApprovedAt().format(DATE_FORMATTER) : "");
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
            columns.computeIfAbsent(colIndex, k -> new ArrayList<>()).add(value);
        }
    }

    private void clearAndWriteAll(
        String spreadsheetId,
        List<ClubMember> members,
        Map<Integer, ClubFeePayment> paymentMap
    ) throws IOException {
        String clearRange = "A:H";
        googleSheetsService.spreadsheets().values()
            .clear(spreadsheetId, clearRange, new ClearValuesRequest())
            .execute();

        List<Object> headerRow = List.of(
            "Name", "StudentId", "Email", "Phone", "Position", "JoinedAt", "FeePaid", "PaidAt"
        );
        List<List<Object>> rows = new ArrayList<>();
        rows.add(headerRow);

        for (ClubMember member : members) {
            Integer userId = member.getUser().getId();
            ClubFeePayment payment = paymentMap.get(userId);
            String phone = member.getUser().getPhoneNumber() != null
                ? "'" + member.getUser().getPhoneNumber() : "";
            String feePaid = payment != null && payment.isPaid() ? "Y" : "N";
            String paidAt = payment != null && payment.getApprovedAt() != null
                ? payment.getApprovedAt().format(DATE_FORMATTER) : "";

            rows.add(List.of(
                member.getUser().getName(),
                member.getUser().getStudentNumber(),
                member.getUser().getEmail(),
                phone,
                member.getClubPosition().getDescription(),
                member.getCreatedAt().format(DATE_FORMATTER),
                feePaid,
                paidAt
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

    private List<ClubMember> sort(
        List<ClubMember> members,
        Map<Integer, ClubFeePayment> paymentMap,
        ClubSheetSortKey sortKey,
        boolean ascending
    ) {
        Comparator<ClubMember> comparator = switch (sortKey) {
            case NAME -> Comparator.comparing(m -> m.getUser().getName());
            case STUDENT_ID -> Comparator.comparing(m -> m.getUser().getStudentNumber());
            case POSITION -> Comparator.comparingInt(m -> m.getClubPosition().getPriority());
            case JOINED_AT -> Comparator.comparing(ClubMember::getCreatedAt);
            case FEE_PAID -> Comparator.comparing(m -> {
                ClubFeePayment p = paymentMap.get(m.getUser().getId());
                return p != null && p.isPaid() ? 0 : 1;
            });
        };

        if (!ascending) {
            comparator = comparator.reversed();
        }

        return members.stream().sorted(comparator).toList();
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
}
