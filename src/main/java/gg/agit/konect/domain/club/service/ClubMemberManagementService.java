package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPositionGroup.MANAGER;
import static gg.agit.konect.domain.club.enums.ClubPositionGroup.PRESIDENT;
import static gg.agit.konect.domain.club.enums.ClubPositionGroup.VICE_PRESIDENT;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPosition;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPositionRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberManagementService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPositionRepository clubPositionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void changeMemberPosition(
        Integer clubId,
        Integer targetUserId,
        Integer requesterId,
        MemberPositionChangeRequest request
    ) {
        Club club = clubRepository.getById(clubId);

        if (targetUserId.equals(requesterId)) {
            throw CustomException.of(CANNOT_CHANGE_OWN_POSITION);
        }

        ClubMember requester = clubMemberRepository.findByClubIdAndUserId(clubId, requesterId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_MEMBER));

        ClubMember target = clubMemberRepository.findByClubIdAndUserId(clubId, targetUserId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_MEMBER));

        if (!requester.canManage(target)) {
            throw CustomException.of(CANNOT_MANAGE_HIGHER_POSITION);
        }

        ClubPosition newPosition = clubPositionRepository.getById(request.positionId());

        if (!newPosition.getClub().getId().equals(clubId)) {
            throw CustomException.of(NOT_FOUND_CLUB_POSITION);
        }

        ClubPositionGroup newPositionGroup = newPosition.getClubPositionGroup();

        if (!requester.getPositionGroup().canManage(newPositionGroup)) {
            throw CustomException.of(FORBIDDEN_MEMBER_POSITION_CHANGE);
        }

        if (newPositionGroup == VICE_PRESIDENT) {
            long vicePresidentCount = clubMemberRepository.countByClubIdAndPositionGroup(clubId, VICE_PRESIDENT);
            if (vicePresidentCount >= 1) {
                throw CustomException.of(VICE_PRESIDENT_ALREADY_EXISTS);
            }
        }

        if (newPositionGroup == MANAGER) {
            long managerCount = clubMemberRepository.countByClubIdAndPositionGroup(clubId, MANAGER);
            if (managerCount >= 20) {
                throw CustomException.of(MANAGER_LIMIT_EXCEEDED);
            }
        }

        target.changePosition(newPosition);
    }

    @Transactional
    public void transferPresident(
        Integer clubId,
        Integer currentPresidentId,
        PresidentTransferRequest request
    ) {
        Club club = clubRepository.getById(clubId);

        ClubMember currentPresident = clubMemberRepository.findByClubIdAndUserId(clubId, currentPresidentId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_PRESIDENT));

        if (!currentPresident.isPresident()) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        Integer newPresidentUserId = request.newPresidentUserId();

        if (currentPresidentId.equals(newPresidentUserId)) {
            throw CustomException.of(ILLEGAL_ARGUMENT);
        }

        ClubMember newPresident = clubMemberRepository.findByClubIdAndUserId(clubId, newPresidentUserId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_MEMBER));

        ClubPosition presidentPosition = clubPositionRepository.findFirstByClubIdAndPositionGroup(clubId, PRESIDENT)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_POSITION));

        ClubPosition memberPosition = clubPositionRepository.findFirstByClubIdAndPositionGroup(clubId, ClubPositionGroup.MEMBER)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_POSITION));

        currentPresident.changePosition(memberPosition);
        newPresident.changePosition(presidentPosition);
    }

    @Transactional
    public void changeVicePresident(
        Integer clubId,
        Integer requesterId,
        VicePresidentChangeRequest request
    ) {
        Club club = clubRepository.getById(clubId);

        ClubMember requester = clubMemberRepository.findByClubIdAndUserId(clubId, requesterId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_MEMBER));

        if (!requester.isPresident()) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }

        ClubPosition vicePresidentPosition = clubPositionRepository.findFirstByClubIdAndPositionGroup(clubId, VICE_PRESIDENT)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_POSITION));

        Optional<ClubMember> currentVicePresidentOpt = clubMemberRepository.findAllByClubIdAndPositionGroup(clubId, VICE_PRESIDENT)
            .stream()
            .findFirst();

        Integer newVicePresidentUserId = request.vicePresidentUserId();

        if (newVicePresidentUserId == null) {
            if (currentVicePresidentOpt.isPresent()) {
                ClubMember currentVicePresident = currentVicePresidentOpt.get();
                ClubPosition memberPosition = clubPositionRepository.findFirstByClubIdAndPositionGroup(clubId, ClubPositionGroup.MEMBER)
                    .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_POSITION));
                currentVicePresident.changePosition(memberPosition);
            }
            return;
        }

        if (requesterId.equals(newVicePresidentUserId)) {
            throw CustomException.of(CANNOT_CHANGE_OWN_POSITION);
        }

        ClubMember newVicePresident = clubMemberRepository.findByClubIdAndUserId(clubId, newVicePresidentUserId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_MEMBER));

        if (currentVicePresidentOpt.isPresent()) {
            ClubMember currentVicePresident = currentVicePresidentOpt.get();
            if (!currentVicePresident.getId().getUserId().equals(newVicePresidentUserId)) {
                ClubPosition memberPosition = clubPositionRepository.findFirstByClubIdAndPositionGroup(clubId, ClubPositionGroup.MEMBER)
                    .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_POSITION));
                currentVicePresident.changePosition(memberPosition);
            }
        }

        newVicePresident.changePosition(vicePresidentPosition);
    }

    @Transactional
    public void removeMember(Integer clubId, Integer targetUserId, Integer requesterId) {
        Club club = clubRepository.getById(clubId);

        if (targetUserId.equals(requesterId)) {
            throw CustomException.of(CANNOT_REMOVE_SELF);
        }

        ClubMember requester = clubMemberRepository.findByClubIdAndUserId(clubId, requesterId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_MEMBER));

        ClubMember target = clubMemberRepository.findByClubIdAndUserId(clubId, targetUserId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_MEMBER));

        if (target.isPresident()) {
            throw CustomException.of(CANNOT_DELETE_CLUB_PRESIDENT);
        }

        if (!requester.canManage(target)) {
            throw CustomException.of(CANNOT_MANAGE_HIGHER_POSITION);
        }

        clubMemberRepository.delete(target);
    }
}
