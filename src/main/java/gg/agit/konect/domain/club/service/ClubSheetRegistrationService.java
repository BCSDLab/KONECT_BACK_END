package gg.agit.konect.domain.club.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubSheetRegistrationService {

    private final ClubRepository clubRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void updateSheetRegistration(
        Integer clubId,
        String spreadsheetId,
        SheetHeaderMapper.SheetAnalysisResult result
    ) {
        Club club = clubRepository.getById(clubId);
        applySheetRegistration(club, spreadsheetId, result);
        clubRepository.save(club);
    }

    private void applySheetRegistration(
        Club club,
        String spreadsheetId,
        SheetHeaderMapper.SheetAnalysisResult result
    ) {
        String mappingJson = serializeMemberListMapping(result);

        club.updateGoogleSheetId(spreadsheetId);
        club.updateSheetColumnMapping(mappingJson);
    }

    private String serializeMemberListMapping(SheetHeaderMapper.SheetAnalysisResult result) {
        SheetColumnMapping memberListMapping = result.memberListMapping();
        if (memberListMapping == null) {
            throw CustomException.of(ApiResponseCode.CLUB_SHEET_ANALYSIS_REQUIRED);
        }

        try {
            return objectMapper.writeValueAsString(memberListMapping.toMap());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize sheet column mapping. cause={}", e.getMessage());
            throw CustomException.of(ApiResponseCode.FAILED_SYNC_GOOGLE_SHEET);
        }
    }
}
