package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BasicFilter;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SetBasicFilterRequest;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubFeePayment;
import gg.agit.konect.domain.club.model.ClubMember;
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
    private static final String CLEAR_RANGE = "A:H";
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<Object> HEADER_ROW = List.of(
        "Name", "StudentId", "Email", "Phone", "Position", "JoinedAt", "FeePaid", "PaidAt"
    );

    private final Sheets googleSheetsService;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubFeePaymentRepository clubFeePaymentRepository;

    @Async("sheetSyncExecutor")
    @Transactional(readOnly = true)
    public void execute(Integer clubId) {
        executeWithSort(clubId, ClubSheetSortKey.POSITION, true);
    }

    @Async("sheetSyncExecutor")
    @Transactional(readOnly = true)
    public void executeWithSort(Integer clubId, ClubSheetSortKey sortKey, boolean ascending) {
        Club club = clubRepository.getById(clubId);
        String spreadsheetId = club.getGoogleSheetId();
        if (spreadsheetId == null || spreadsheetId.isBlank()) {
            return;
        }

        List<ClubMember> members = clubMemberRepository.findAllByClubId(clubId);
        List<ClubFeePayment> payments = clubFeePaymentRepository.findAllByClubId(clubId);

        Map<Integer, ClubFeePayment> paymentMap = payments.stream()
            .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        List<ClubMember> sorted = sort(members, paymentMap, sortKey, ascending);

        try {
            clearSheet(spreadsheetId);
            writeSheet(spreadsheetId, buildRows(sorted, paymentMap));
            applyFormat(spreadsheetId);
        } catch (IOException e) {
            log.error(
                "Sheet sync failed. clubId={}, spreadsheetId={}, cause={}",
                clubId, spreadsheetId, e.getMessage(), e
            );
        }

        log.info("Sheet sync done. clubId={}, members={}", clubId, members.size());
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

    private List<List<Object>> buildRows(
        List<ClubMember> members,
        Map<Integer, ClubFeePayment> paymentMap
    ) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(HEADER_ROW);

        for (ClubMember member : members) {
            Integer userId = member.getUser().getId();
            ClubFeePayment payment = paymentMap.get(userId);

            String feePaid = payment != null && payment.isPaid() ? "Y" : "N";
            String paidAt = (payment != null && payment.getApprovedAt() != null)
                ? payment.getApprovedAt().format(DATE_FORMATTER) : "";
            String phone = member.getUser().getPhoneNumber() != null
                ? "'" + member.getUser().getPhoneNumber() : "";

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

        return rows;
    }
}
