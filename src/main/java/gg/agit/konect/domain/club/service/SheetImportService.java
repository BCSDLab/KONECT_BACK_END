package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
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

    @Transactional
    public SheetImportResponse importPreMembersFromSheet(
        Integer clubId,
        Integer requesterId,
        String spreadsheetUrl
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);
        Club club = clubRepository.getById(clubId);
        Integer universityId = club.getUniversity().getId();

        String spreadsheetId = SpreadsheetUrlParser.extractId(spreadsheetUrl);

        SheetHeaderMapper.SheetAnalysisResult analysis =
            sheetHeaderMapper.analyzeAllSheets(spreadsheetId);
        SheetColumnMapping mapping = analysis.memberListMapping();

        List<List<Object>> rows = readDataRows(spreadsheetId, mapping);

        // N+1 방지: 루프 전 기존 부원 / 사전 회원 정보 일괄 조회
        Set<String> existingMemberStudentNumbers =
            clubMemberRepository.findStudentNumbersByClubId(clubId);
        Set<String> existingPreMemberKeys = buildPreMemberKeySet(clubId);

        // 시트에 등장하는 모든 학번 수집 (bulk 조회용)
        Set<String> allStudentNumbers = rows.stream()
            .map(row -> getCell(row, mapping, SheetColumnMapping.STUDENT_ID))
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());

        // 대학 + 학번 IN 으로 users 일괄 조회 → Map<학번, List<User>>
        Map<String, List<User>> usersByStudentNumber = new HashMap<>();
        if (!allStudentNumbers.isEmpty()) {
            userRepository.findAllByUniversityIdAndStudentNumberIn(universityId, allStudentNumbers)
                .forEach(u -> usersByStudentNumber
                    .computeIfAbsent(u.getStudentNumber(), k -> new ArrayList<>())
                    .add(u));
        }

        List<String> warnings = new ArrayList<>();
        int presidentCount = 0;
        int imported = 0;
        int autoRegistered = 0;

        for (List<Object> row : rows) {
            String name = getCell(row, mapping, SheetColumnMapping.NAME);
            String studentNumber = getCell(row, mapping, SheetColumnMapping.STUDENT_ID);

            if (name.isBlank() || studentNumber.isBlank()) {
                continue;
            }

            // 전화번호 형식 유효성 경고 (phone 칸이 있고 값도 있는데 전화번호처럼 안 보이면)
            String phone = getCell(row, mapping, SheetColumnMapping.PHONE);
            if (!phone.isBlank() && !PhoneNumberNormalizer.looksLikePhoneNumber(phone)) {
                warnings.add(String.format(
                    "전화번호 형식이 올바르지 않습니다 - 학번: %s, 이름: %s, 입력값: '%s'",
                    studentNumber, name, phone
                ));
            }

            String positionStr = getCell(row, mapping, SheetColumnMapping.POSITION);
            ClubPosition position = resolvePosition(positionStr);

            // 회장 중복 감지
            if (position == ClubPosition.PRESIDENT) {
                presidentCount++;
                if (presidentCount > 1) {
                    warnings.add(String.format(
                        "회장이 2명 이상 등록되어 있습니다 - 중복 회장: 학번 %s, 이름 %s",
                        studentNumber, name
                    ));
                }
            }

            // 이미 club_member에 있는 학번은 스킵
            if (existingMemberStudentNumbers.contains(studentNumber)) {
                continue;
            }

            // 이미 club_pre_member에 있는 (학번+이름) 조합은 스킵
            if (existingPreMemberKeys.contains(preMemberKey(studentNumber, name))) {
                continue;
            }

            // users 테이블에서 동일 대학 + 학번으로 매칭, 이름까지 일치하는 유저 탐색
            List<User> candidates = usersByStudentNumber.getOrDefault(studentNumber, List.of());
            List<User> matched = candidates.stream()
                .filter(u -> name.equals(u.getName()))
                .toList();

            if (matched.size() == 1) {
                // 정확히 1명 매칭 → club_member 직접 등록
                User matchedUser = matched.get(0);
                if (!clubMemberRepository.existsByClubIdAndUserId(clubId, matchedUser.getId())) {
                    // 기존 pre_member에 동일 학번이 있으면 제거
                    clubPreMemberRepository.deleteByClubIdAndStudentNumber(
                        clubId, matchedUser.getStudentNumber()
                    );
                    ClubMember clubMember = ClubMember.builder()
                        .club(club)
                        .user(matchedUser)
                        .clubPosition(position)
                        .build();
                    ClubMember saved = clubMemberRepository.save(clubMember);
                    chatRoomMembershipService.addClubMember(saved);
                    existingMemberStudentNumbers.add(studentNumber);
                    autoRegistered++;
                }
                continue;
            }

            if (matched.size() > 1) {
                // 동명이인 2명 이상 → 모호성 경고, pre_member로 등록
                warnings.add(String.format(
                    "동명이인이 여러 명 존재하여 자동 매칭할 수 없습니다 - 학번: %s, 이름: %s",
                    studentNumber, name
                ));
            }

            // users 미매칭 또는 동명이인 → pre_member 등록
            ClubPreMember preMember = ClubPreMember.builder()
                .club(club)
                .studentNumber(studentNumber)
                .name(name)
                .clubPosition(position)
                .build();
            clubPreMemberRepository.save(preMember);
            existingPreMemberKeys.add(preMemberKey(studentNumber, name));
            imported++;
        }

        log.info(
            "Sheet import done. clubId={}, spreadsheetId={}, imported={}, autoRegistered={}, warnings={}",
            clubId, spreadsheetId, imported, autoRegistered, warnings.size()
        );
        return SheetImportResponse.of(imported, autoRegistered, warnings);
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

    private Set<String> buildPreMemberKeySet(Integer clubId) {
        Set<String> keys = new HashSet<>();
        clubPreMemberRepository.findStudentNumberAndNameByClubId(clubId)
            .forEach(k -> keys.add(preMemberKey(k.getStudentNumber(), k.getName())));
        return keys;
    }

    private String preMemberKey(String studentNumber, String name) {
        return studentNumber + "\u0000" + name;
    }
}
