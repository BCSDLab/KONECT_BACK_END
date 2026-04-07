package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.dto.SheetImportConfirmRequest;
import gg.agit.konect.domain.club.dto.SheetImportPreviewResponse;
import gg.agit.konect.domain.club.dto.SheetImportResponse;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.util.PhoneNumberNormalizer;
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
    private final UserRepository userRepository;
    private final ChatRoomMembershipService chatRoomMembershipService;
    private final ClubPermissionValidator clubPermissionValidator;
    private final PlatformTransactionManager transactionManager;

    public SheetImportPreviewResponse previewPreMembersFromSheet(
        Integer clubId,
        Integer requesterId,
        String spreadsheetUrl
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);

        String spreadsheetId = SpreadsheetUrlParser.extractId(spreadsheetUrl);
        SheetHeaderMapper.SheetAnalysisResult analysis =
            sheetHeaderMapper.analyzeAllSheets(spreadsheetId);
        SheetImportSource source = loadSheetImportSource(spreadsheetId, analysis.memberListMapping());
        return executeReadOnlyTransaction(() -> {
            Club club = clubRepository.getById(clubId);
            SheetImportPlan plan = buildImportPlan(
                clubId,
                club,
                source.members(),
                source.warnings()
            );
            return SheetImportPreviewResponse.of(plan.previewMembers(), plan.warnings());
        });
    }

    @Transactional
    public SheetImportResponse confirmImportPreMembers(
        Integer clubId,
        Integer requesterId,
        List<SheetImportConfirmRequest.ConfirmMember> members
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);
        Club club = clubRepository.getById(clubId);

        SheetImportPlan plan = buildImportPlan(
            clubId,
            club,
            toImportMembers(members),
            List.of()
        );
        applyImportPlan(clubId, "preview-confirm", plan);
        return SheetImportResponse.of(
            plan.preRegisteredCount(),
            plan.autoRegisteredCount(),
            plan.warnings()
        );
    }

    public SheetImportResponse importPreMembersFromSheet(
        Integer clubId,
        Integer requesterId,
        String spreadsheetUrl
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);

        String spreadsheetId = SpreadsheetUrlParser.extractId(spreadsheetUrl);
        SheetHeaderMapper.SheetAnalysisResult analysis =
            sheetHeaderMapper.analyzeAllSheets(spreadsheetId);
        SheetImportSource source = loadSheetImportSource(spreadsheetId, analysis.memberListMapping());
        return executeTransaction(() -> importPreMembers(
            clubId,
            spreadsheetId,
            source
        ));
    }

    SheetImportResponse importPreMembersFromSheet(
        Integer clubId,
        Integer requesterId,
        String spreadsheetId,
        SheetColumnMapping mapping
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);
        SheetImportSource source = loadSheetImportSource(spreadsheetId, mapping);
        return executeTransaction(() -> importPreMembers(
            clubId,
            spreadsheetId,
            source
        ));
    }

    private SheetImportResponse importPreMembers(
        Integer clubId,
        String importSource,
        SheetImportSource source
    ) {
        Club club = clubRepository.getById(clubId);
        SheetImportPlan plan = buildImportPlan(
            clubId,
            club,
            source.members(),
            source.warnings()
        );
        applyImportPlan(clubId, importSource, plan);
        return SheetImportResponse.of(
            plan.preRegisteredCount(),
            plan.autoRegisteredCount(),
            plan.warnings()
        );
    }

    private void applyImportPlan(
        Integer clubId,
        String importSource,
        SheetImportPlan plan
    ) {
        if (!plan.studentNumbersToCleanFromPre().isEmpty()) {
            clubPreMemberRepository.deleteByClubIdAndStudentNumberIn(
                clubId,
                plan.studentNumbersToCleanFromPre()
            );
        }

        List<ClubMember> savedMembers = plan.clubMembersToSave().isEmpty()
            ? List.of()
            : clubMemberRepository.saveAll(plan.clubMembersToSave());

        for (ClubMember savedMember : savedMembers) {
            chatRoomMembershipService.addClubMember(savedMember);
        }

        if (!plan.preMembersToSave().isEmpty()) {
            clubPreMemberRepository.saveAll(plan.preMembersToSave());
        }

        log.info(
            "Sheet import done. clubId={}, source={}, imported={}, autoRegistered={}, warnings={}",
            clubId,
            importSource,
            plan.preRegisteredCount(),
            plan.autoRegisteredCount(),
            plan.warnings().size()
        );
    }

    private SheetImportPlan buildImportPlan(
        Integer clubId,
        Club club,
        List<ImportMember> members,
        List<String> initialWarnings
    ) {
        Integer universityId = club.getUniversity().getId();

        Set<String> existingMemberStudentNumbers =
            new HashSet<>(clubMemberRepository.findStudentNumbersByClubId(clubId));
        Set<String> existingPreMemberKeys = buildPreMemberKeySet(clubId);
        Set<Integer> existingMemberUserIds =
            new HashSet<>(clubMemberRepository.findUserIdsByClubId(clubId));

        Set<String> allStudentNumbers = members.stream()
            .map(ImportMember::studentNumber)
            .filter(studentNumber -> !studentNumber.isBlank())
            .collect(Collectors.toSet());

        Map<String, List<User>> usersByStudentNumber = new HashMap<>();
        if (!allStudentNumbers.isEmpty()) {
            userRepository.findAllByUniversityIdAndStudentNumberIn(universityId, allStudentNumbers)
                .forEach(user -> usersByStudentNumber
                    .computeIfAbsent(user.getStudentNumber(), key -> new ArrayList<>())
                    .add(user));
        }

        List<SheetImportPreviewResponse.PreviewMember> previewMembers = new ArrayList<>();
        List<ClubMember> clubMembersToSave = new ArrayList<>();
        Set<String> studentNumbersToCleanFromPre = new HashSet<>();
        List<ClubPreMember> preMembersToSave = new ArrayList<>();
        List<String> warnings = new ArrayList<>(initialWarnings);
        int presidentCount = 0;

        for (ImportMember importMember : members) {
            String name = importMember.name();
            String studentNumber = importMember.studentNumber();
            ClubPosition position = importMember.clubPosition() == null
                ? ClubPosition.MEMBER
                : importMember.clubPosition();

            if (name.isBlank() || studentNumber.isBlank()) {
                continue;
            }

            if (position == ClubPosition.PRESIDENT) {
                presidentCount++;
                if (presidentCount > 1) {
                    warnings.add(String.format(
                        "회장이 2명 이상 등록되어 있습니다 - 중복 회장: 학번 %s, 이름 %s",
                        studentNumber,
                        name
                    ));
                }
            }

            if (existingMemberStudentNumbers.contains(studentNumber)) {
                continue;
            }

            List<User> candidates = usersByStudentNumber.getOrDefault(studentNumber, List.of());
            List<User> matchedUsers = candidates.stream()
                .filter(user -> user.getName() != null && name.equalsIgnoreCase(user.getName().trim()))
                .toList();

            if (matchedUsers.size() == 1) {
                User matchedUser = matchedUsers.get(0);
                if (!existingMemberUserIds.contains(matchedUser.getId())) {
                    ClubMember clubMember = ClubMember.builder()
                        .club(club)
                        .user(matchedUser)
                        .clubPosition(position)
                        .build();

                    clubMembersToSave.add(clubMember);
                    previewMembers.add(SheetImportPreviewResponse.PreviewMember.from(clubMember));
                    studentNumbersToCleanFromPre.add(matchedUser.getStudentNumber());
                    existingMemberStudentNumbers.add(studentNumber);
                    existingMemberUserIds.add(matchedUser.getId());
                    existingPreMemberKeys.remove(preMemberKey(studentNumber, name));
                }
                continue;
            }

            if (matchedUsers.size() > 1) {
                warnings.add(String.format(
                    "동명이인이 여러 명 존재하여 자동 매칭할 수 없습니다 - 학번: %s, 이름: %s",
                    studentNumber,
                    name
                ));
            }

            if (existingPreMemberKeys.contains(preMemberKey(studentNumber, name))) {
                continue;
            }

            ClubPreMember preMember = ClubPreMember.builder()
                .club(club)
                .studentNumber(studentNumber)
                .name(name)
                .clubPosition(position)
                .build();

            preMembersToSave.add(preMember);
            previewMembers.add(SheetImportPreviewResponse.PreviewMember.from(preMember));
            existingPreMemberKeys.add(preMemberKey(studentNumber, name));
        }

        return new SheetImportPlan(
            previewMembers,
            clubMembersToSave,
            studentNumbersToCleanFromPre,
            preMembersToSave,
            warnings
        );
    }

    private SheetImportSource loadSheetImportSource(String spreadsheetId, SheetColumnMapping mapping) {
        List<List<Object>> rows = readDataRows(spreadsheetId, mapping);
        List<ImportMember> members = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (List<Object> row : rows) {
            String name = getCell(row, mapping, SheetColumnMapping.NAME);
            String studentNumber = getCell(row, mapping, SheetColumnMapping.STUDENT_ID);

            if (name.isBlank() || studentNumber.isBlank()) {
                continue;
            }

            String phone = getCell(row, mapping, SheetColumnMapping.PHONE);
            if (!phone.isBlank() && !PhoneNumberNormalizer.looksLikePhoneNumber(phone)) {
                warnings.add(String.format(
                    "전화번호 형식이 올바르지 않습니다 - 학번: %s, 이름: %s, 입력값: '%s'",
                    studentNumber,
                    name,
                    phone
                ));
            }

            String positionText = getCell(row, mapping, SheetColumnMapping.POSITION);
            members.add(new ImportMember(
                studentNumber,
                name,
                resolvePosition(positionText)
            ));
        }

        return new SheetImportSource(members, warnings);
    }

    private List<ImportMember> toImportMembers(List<SheetImportConfirmRequest.ConfirmMember> members) {
        if (members == null) {
            return List.of();
        }

        return members.stream()
            .filter(SheetImportConfirmRequest.ConfirmMember::isEnabled)
            .map(member -> new ImportMember(
                member.studentNumber().trim(),
                member.name().trim(),
                member.clubPosition() == null ? ClubPosition.MEMBER : member.clubPosition()
            ))
            .toList();
    }

    private <T> T executeTransaction(Supplier<T> action) {
        return executeWithTemplate(false, action);
    }

    private <T> T executeReadOnlyTransaction(Supplier<T> action) {
        return executeWithTemplate(true, action);
    }

    private <T> T executeWithTemplate(boolean readOnly, Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(readOnly);
        return Objects.requireNonNull(transactionTemplate.execute(status -> action.get()));
    }

    private List<List<Object>> readDataRows(String spreadsheetId, SheetColumnMapping mapping) {
        try {
            int dataStartRow = mapping.getDataStartRow();
            String range = "A" + dataStartRow + ":Z";
            ValueRange response = googleSheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .setValueRenderOption("FORMATTED_VALUE")
                .execute();

            List<List<Object>> values = response.getValues();
            return values != null ? values : List.of();
        } catch (IOException e) {
            if (GoogleSheetApiExceptionHelper.isAccessDenied(e)) {
                log.warn(
                    "Google Sheets access denied while reading sheet data. spreadsheetId={}, cause={}",
                    spreadsheetId,
                    e.getMessage()
                );
                throw GoogleSheetApiExceptionHelper.accessDenied();
            }
            log.error("Failed to read sheet data. spreadsheetId={}", spreadsheetId, e);
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
        }
    }

    private String getCell(List<Object> row, SheetColumnMapping mapping, String field) {
        int columnIndex = mapping.getColumnIndex(field);
        if (columnIndex < 0 || columnIndex >= row.size()) {
            return "";
        }

        String value = row.get(columnIndex).toString().trim();
        if (value.startsWith("'")) {
            return value.substring(1);
        }
        return value;
    }

    private ClubPosition resolvePosition(String positionText) {
        for (ClubPosition position : ClubPosition.values()) {
            if (position.getDescription().equals(positionText)
                || position.name().equalsIgnoreCase(positionText)) {
                return position;
            }
        }
        return ClubPosition.MEMBER;
    }

    private Set<String> buildPreMemberKeySet(Integer clubId) {
        Set<String> keys = new HashSet<>();
        clubPreMemberRepository.findStudentNumberAndNameByClubId(clubId)
            .forEach(key -> keys.add(preMemberKey(key.getStudentNumber(), key.getName())));
        return keys;
    }

    private String preMemberKey(String studentNumber, String name) {
        return studentNumber + "\u0000" + name;
    }

    private record SheetImportPlan(
        List<SheetImportPreviewResponse.PreviewMember> previewMembers,
        List<ClubMember> clubMembersToSave,
        Set<String> studentNumbersToCleanFromPre,
        List<ClubPreMember> preMembersToSave,
        List<String> warnings
    ) {
        private int autoRegisteredCount() {
            return clubMembersToSave.size();
        }

        private int preRegisteredCount() {
            return preMembersToSave.size();
        }
    }

    private record SheetImportSource(
        List<ImportMember> members,
        List<String> warnings
    ) {
    }

    private record ImportMember(
        String studentNumber,
        String name,
        ClubPosition clubPosition
    ) {
    }
}
