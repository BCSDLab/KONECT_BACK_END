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

        // N+1 방지: 루프 전 기존 부원 학번 Set / 사전 회원 key Set / 부원 userId Set 일괄 조회
        Set<String> existingMemberStudentNumbers =
            clubMemberRepository.findStudentNumbersByClubId(clubId);
        Set<String> existingPreMemberKeys = buildPreMemberKeySet(clubId);
        Set<Integer> existingMemberUserIds =
            new HashSet<>(clubMemberRepository.findUserIdsByClubId(clubId));

        // 시트에 등장하는 모든 학번 수집 → users 일괄 조회
        Set<String> allStudentNumbers = rows.stream()
            .map(row -> getCell(row, mapping, SheetColumnMapping.STUDENT_ID))
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());

        Map<String, List<User>> usersByStudentNumber = new HashMap<>();
        if (!allStudentNumbers.isEmpty()) {
            userRepository.findAllByUniversityIdAndStudentNumberIn(universityId, allStudentNumbers)
                .forEach(u -> usersByStudentNumber
                    .computeIfAbsent(u.getStudentNumber(), k -> new ArrayList<>())
                    .add(u));
        }

        // 루프에서 수집할 배치 작업 대상
        List<ClubMember> clubMembersToSave = new ArrayList<>();
        Set<String> studentNumbersToCleanFromPre = new HashSet<>();
        List<ClubPreMember> preMembersToSave = new ArrayList<>();

        List<String> warnings = new ArrayList<>();
        int presidentCount = 0;

        for (List<Object> row : rows) {
            String name = getCell(row, mapping, SheetColumnMapping.NAME);
            String studentNumber = getCell(row, mapping, SheetColumnMapping.STUDENT_ID);

            if (name.isBlank() || studentNumber.isBlank()) {
                continue;
            }

            // 전화번호 형식 유효성 경고
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

            // users 테이블에서 동일 대학 + 학번으로 매칭, 이름까지 일치하는 유저 탐색
            // trim() / equalsIgnoreCase로 공백·대소문자 차이 허용
            // 주의: existingPreMemberKeys 체크보다 먼저 수행하여
            // 이미 pre_member로 등록된 행도 User 생성 후 재-import 시 club_member로 승격 가능하게 함
            List<User> candidates = usersByStudentNumber.getOrDefault(studentNumber, List.of());
            List<User> matched = candidates.stream()
                .filter(u -> name != null && u.getName() != null
                    && name.trim().equalsIgnoreCase(u.getName().trim()))
                .toList();

            if (matched.size() == 1) {
                User matchedUser = matched.get(0);
                // userId Set으로 중복 체크 (N+1 없음)
                if (!existingMemberUserIds.contains(matchedUser.getId())) {
                    // 기존 pre_member 행도 함께 정리 (중복 방지)
                    studentNumbersToCleanFromPre.add(matchedUser.getStudentNumber());
                    clubMembersToSave.add(ClubMember.builder()
                        .club(club)
                        .user(matchedUser)
                        .clubPosition(position)
                        .build());
                    existingMemberStudentNumbers.add(studentNumber);
                    existingMemberUserIds.add(matchedUser.getId());
                    existingPreMemberKeys.remove(preMemberKey(studentNumber, name));
                }
                continue;
            }

            if (matched.size() > 1) {
                warnings.add(String.format(
                    "동명이인이 여러 명 존재하여 자동 매칭할 수 없습니다 - 학번: %s, 이름: %s",
                    studentNumber, name
                ));
            }

            // users 미매칭 또는 동명이인 → 이미 pre_member에 있으면 스킵, 없으면 등록
            if (existingPreMemberKeys.contains(preMemberKey(studentNumber, name))) {
                continue;
            }

            preMembersToSave.add(ClubPreMember.builder()
                .club(club)
                .studentNumber(studentNumber)
                .name(name)
                .clubPosition(position)
                .build());
            existingPreMemberKeys.add(preMemberKey(studentNumber, name));
        }

        // 배치 처리: pre_member 정리 → club_member 일괄 저장 → 채팅방 등록
        if (!studentNumbersToCleanFromPre.isEmpty()) {
            clubPreMemberRepository.deleteByClubIdAndStudentNumberIn(
                clubId, studentNumbersToCleanFromPre
            );
        }
        List<ClubMember> savedMembers = clubMembersToSave.isEmpty()
            ? List.of()
            : clubMemberRepository.saveAll(clubMembersToSave);

        for (ClubMember saved : savedMembers) {
            chatRoomMembershipService.addClubMember(saved);
        }

        if (!preMembersToSave.isEmpty()) {
            clubPreMemberRepository.saveAll(preMembersToSave);
        }

        int autoRegistered = savedMembers.size();
        int imported = preMembersToSave.size();

        log.info(
            "Sheet import done. clubId={}, spreadsheetId={}, imported={}, autoRegistered={}, "
                + "warnings={}",
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
                .setValueRenderOption("FORMATTED_VALUE")
                .execute();

            List<List<Object>> values = response.getValues();
            return values != null ? values : List.of();

        } catch (IOException e) {
            log.error("Failed to read sheet data. spreadsheetId={}", spreadsheetId, e);
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
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
