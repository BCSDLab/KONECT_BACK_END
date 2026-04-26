package gg.agit.konect.domain.club.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubRepository;
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
        String mappingJson = null;
        try {
            mappingJson = objectMapper.writeValueAsString(result.memberListMapping().toMap());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize mapping, skipping. cause={}", e.getMessage());
        }

        club.updateGoogleSheetId(spreadsheetId);
        if (mappingJson != null) {
            club.updateSheetColumnMapping(mappingJson);
        }
    }
}
