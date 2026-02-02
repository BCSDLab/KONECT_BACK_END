package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPositionGroup.LEADERS;
import static gg.agit.konect.domain.club.enums.ClubPositionGroup.MANAGERS;
import static gg.agit.konect.domain.club.enums.ClubPositionGroup.PRESIDENT_ONLY;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS;

import java.util.Set;

import org.springframework.stereotype.Component;

import gg.agit.konect.domain.club.enums.ClubPositionGroup;
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

    private boolean hasAccess(Integer clubId, Integer userId, Set<ClubPositionGroup> allowedGroups) {
        return clubMemberRepository.existsByClubIdAndUserIdAndPositionGroupIn(clubId, userId, allowedGroups);
    }
}
