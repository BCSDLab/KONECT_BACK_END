package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPosition.LEADERS;
import static gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS;
import static gg.agit.konect.domain.club.enums.ClubPosition.PRESIDENT_ONLY;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.util.Set;

import org.springframework.stereotype.Component;

import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClubPermissionValidator {

    private final ClubMemberRepository clubMemberRepository;

    public void validatePresidentAccess(Integer clubId, Integer userId) {
        if (!hasAccess(clubId, userId, PRESIDENT_ONLY)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }
    }

    public void validateLeaderAccess(Integer clubId, Integer userId) {
        if (!hasAccess(clubId, userId, LEADERS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }
    }

    public void validateManagerAccess(Integer clubId, Integer userId) {
        if (!hasAccess(clubId, userId, MANAGERS)) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }
    }

    private boolean hasAccess(Integer clubId, Integer userId, Set<ClubPosition> allowedPositions) {
        return clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(clubId, userId, allowedPositions);
    }
}
