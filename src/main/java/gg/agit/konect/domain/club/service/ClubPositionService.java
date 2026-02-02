package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPositionGroup.PRESIDENT;
import static gg.agit.konect.domain.club.enums.ClubPositionGroup.VICE_PRESIDENT;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubPositionCreateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionUpdateRequest;
import gg.agit.konect.domain.club.dto.ClubPositionsResponse;
import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubPosition;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPositionRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPositionService {

    private static final Set<ClubPositionGroup> MANAGER_ALLOWED_GROUPS =
        EnumSet.of(PRESIDENT, VICE_PRESIDENT);

    private final ClubRepository clubRepository;
    private final ClubPositionRepository clubPositionRepository;
    private final ClubMemberRepository clubMemberRepository;

    public ClubPositionsResponse getClubPositions(Integer clubId, Integer userId) {
        Club club = clubRepository.getById(clubId);

        List<ClubPosition> positions = clubPositionRepository.findAllByClubId(clubId);

        List<ClubPositionsResponse.InnerClubPosition> positionResponses = positions.stream()
            .map(position -> {
                long memberCount = clubMemberRepository.countByPositionId(position.getId());
                return ClubPositionsResponse.InnerClubPosition.of(position, memberCount);
            })
            .toList();

        return ClubPositionsResponse.of(positionResponses);
    }

    @Transactional
    public ClubPositionsResponse createClubPosition(
        Integer clubId,
        Integer userId,
        ClubPositionCreateRequest request
    ) {
        Club club = clubRepository.getById(clubId);

        validateManagerPermission(clubId, userId);

        ClubPositionGroup positionGroup = request.positionGroup();
        if (positionGroup == ClubPositionGroup.PRESIDENT || positionGroup == ClubPositionGroup.VICE_PRESIDENT) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        if (clubPositionRepository.existsByClubIdAndName(clubId, request.name())) {
            throw CustomException.of(POSITION_NAME_DUPLICATED);
        }

        ClubPosition newPosition = ClubPosition.builder()
            .name(request.name())
            .clubPositionGroup(positionGroup)
            .club(club)
            .build();

        clubPositionRepository.save(newPosition);

        return getClubPositions(clubId, userId);
    }

    @Transactional
    public ClubPositionsResponse updateClubPositionName(
        Integer clubId,
        Integer positionId,
        Integer userId,
        ClubPositionUpdateRequest request
    ) {
        clubRepository.getById(clubId);

        validateManagerPermission(clubId, userId);

        ClubPosition position = clubPositionRepository.getById(positionId);

        if (!position.getClub().getId().equals(clubId)) {
            throw CustomException.of(NOT_FOUND_CLUB_POSITION);
        }

        if (!position.canRename()) {
            throw CustomException.of(FORBIDDEN_POSITION_NAME_CHANGE);
        }

        if (clubPositionRepository.existsByClubIdAndNameAndIdNot(clubId, request.name(), positionId)) {
            throw CustomException.of(POSITION_NAME_DUPLICATED);
        }

        position.updateName(request.name());

        return getClubPositions(clubId, userId);
    }

    @Transactional
    public void deleteClubPosition(Integer clubId, Integer positionId, Integer userId) {
        Club club = clubRepository.getById(clubId);

        validateManagerPermission(clubId, userId);

        ClubPosition position = clubPositionRepository.getById(positionId);

        if (!position.getClub().getId().equals(clubId)) {
            throw CustomException.of(NOT_FOUND_CLUB_POSITION);
        }

        if (!position.canDelete()) {
            throw CustomException.of(CANNOT_DELETE_ESSENTIAL_POSITION);
        }

        long memberCount = clubMemberRepository.countByPositionId(positionId);
        if (memberCount > 0) {
            throw CustomException.of(POSITION_IN_USE);
        }

        ClubPositionGroup positionGroup = position.getClubPositionGroup();
        long sameGroupCount = clubPositionRepository.countByClubIdAndClubPositionGroup(clubId, positionGroup);
        if (sameGroupCount < 2) {
            throw CustomException.of(INSUFFICIENT_POSITION_COUNT);
        }

        clubPositionRepository.delete(position);
    }

    private void validateManagerPermission(Integer clubId, Integer userId) {
        boolean hasPermission = clubMemberRepository.existsByClubIdAndUserIdAndPositionGroupIn(
            clubId, userId, MANAGER_ALLOWED_GROUPS
        );

        if (!hasPermission) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }
    }
}
