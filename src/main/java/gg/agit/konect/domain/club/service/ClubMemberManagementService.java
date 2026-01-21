package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPositionGroup.*;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.AddMemberRequest;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.enums.ClubPositionGroup;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPosition;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPositionRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberManagementService {

    public static final int MAX_MANAGER_COUNT = 20;
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
        clubRepository.getById(clubId);

        validateNotSelf(requesterId, targetUserId, CANNOT_CHANGE_OWN_POSITION);

        ClubMember requester = clubMemberRepository.getByClubIdAndUserId(clubId, requesterId);
        validateManagerPermission(requester);

        ClubMember target = clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId);

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
            if (target.getPositionGroup() != VICE_PRESIDENT && vicePresidentCount >= 1) {
                throw CustomException.of(VICE_PRESIDENT_ALREADY_EXISTS);
            }
        }

        if (newPositionGroup == MANAGER) {
            long managerCount = clubMemberRepository.countByClubIdAndPositionGroup(clubId, MANAGER);
            if (target.getPositionGroup() != MANAGER && managerCount >= MAX_MANAGER_COUNT) {
                throw CustomException.of(MANAGER_LIMIT_EXCEEDED);
            }
        }

        target.changePosition(newPosition);
    }

    @Transactional
    public void addMember(
        Integer clubId,
        Integer requesterId,
        AddMemberRequest request
    ) {
        clubRepository.getById(clubId);

        ClubMember requester = clubMemberRepository.getByClubIdAndUserId(clubId, requesterId);
        validateManagerPermission(requester);

        Integer targetUserId = request.userId();

        if (clubMemberRepository.existsByClubIdAndUserId(clubId, targetUserId)) {
            throw CustomException.of(ALREADY_CLUB_MEMBER);
        }

        userRepository.getById(targetUserId);

        ClubPosition position = clubPositionRepository.getById(request.positionId());

        if (!position.getClub().getId().equals(clubId)) {
            throw CustomException.of(NOT_FOUND_CLUB_POSITION);
        }

        ClubPositionGroup positionGroup = position.getClubPositionGroup();

        if (positionGroup == PRESIDENT) {
            throw CustomException.of(FORBIDDEN_MEMBER_POSITION_CHANGE);
        }

        if (positionGroup == VICE_PRESIDENT) {
            long vicePresidentCount = clubMemberRepository.countByClubIdAndPositionGroup(clubId, VICE_PRESIDENT);
            if (vicePresidentCount >= 1) {
                throw CustomException.of(VICE_PRESIDENT_ALREADY_EXISTS);
            }
        }

        if (positionGroup == MANAGER) {
            long managerCount = clubMemberRepository.countByClubIdAndPositionGroup(clubId, MANAGER);
            if (managerCount >= MAX_MANAGER_COUNT) {
                throw CustomException.of(MANAGER_LIMIT_EXCEEDED);
            }
        }

        ClubMember newMember = ClubMember.builder()
            .club(clubRepository.getById(clubId))
            .user(userRepository.getById(targetUserId))
            .clubPosition(position)
            .isFeePaid(false)
            .build();

        clubMemberRepository.save(newMember);
    }

    @Transactional
    public void transferPresident(
        Integer clubId,
        Integer currentPresidentId,
        PresidentTransferRequest request
    ) {
        clubRepository.getById(clubId);

        ClubMember currentPresident = clubMemberRepository.getByClubIdAndUserId(clubId, currentPresidentId);
        validatePresidentPermission(currentPresident);

        Integer newPresidentUserId = request.newPresidentUserId();
        validateNotSelf(currentPresidentId, newPresidentUserId, ILLEGAL_ARGUMENT);

        ClubMember newPresident = clubMemberRepository.getByClubIdAndUserId(clubId, newPresidentUserId);

        ClubPosition presidentPosition = clubPositionRepository.getFirstByClubIdAndClubPositionGroup(clubId, PRESIDENT);
        ClubPosition memberPosition = clubPositionRepository.getFirstByClubIdAndClubPositionGroup(clubId, MEMBER);

        currentPresident.changePosition(memberPosition);
        newPresident.changePosition(presidentPosition);
    }

    @Transactional
    public void changeVicePresident(
        Integer clubId,
        Integer requesterId,
        VicePresidentChangeRequest request
    ) {
        clubRepository.getById(clubId);

        ClubMember requester = clubMemberRepository.getByClubIdAndUserId(clubId, requesterId);
        validatePresidentPermission(requester);

        ClubPosition vicePresidentPosition = clubPositionRepository.getFirstByClubIdAndClubPositionGroup(
            clubId,
            VICE_PRESIDENT
        );

        Optional<ClubMember> currentVicePresidentOpt = clubMemberRepository.findAllByClubIdAndPositionGroup(clubId,
                VICE_PRESIDENT)
            .stream()
            .findFirst();

        Integer newVicePresidentUserId = request.vicePresidentUserId();

        if (newVicePresidentUserId == null) {
            if (currentVicePresidentOpt.isPresent()) {
                ClubMember currentVicePresident = currentVicePresidentOpt.get();
                ClubPosition memberPosition = clubPositionRepository.getFirstByClubIdAndClubPositionGroup(clubId,
                    MEMBER);
                currentVicePresident.changePosition(memberPosition);
            }
            return;
        }

        validateNotSelf(requesterId, newVicePresidentUserId, CANNOT_CHANGE_OWN_POSITION);

        ClubMember newVicePresident = clubMemberRepository.getByClubIdAndUserId(clubId, newVicePresidentUserId);

        if (currentVicePresidentOpt.isPresent()) {
            ClubMember currentVicePresident = currentVicePresidentOpt.get();
            if (!currentVicePresident.getId().getUserId().equals(newVicePresidentUserId)) {
                ClubPosition memberPosition = clubPositionRepository.getFirstByClubIdAndClubPositionGroup(clubId,
                    MEMBER);
                currentVicePresident.changePosition(memberPosition);
            }
        }

        newVicePresident.changePosition(vicePresidentPosition);
    }

    @Transactional
    public void removeMember(Integer clubId, Integer targetUserId, Integer requesterId) {
        clubRepository.getById(clubId);

        validateNotSelf(requesterId, targetUserId, CANNOT_REMOVE_SELF);

        ClubMember requester = clubMemberRepository.getByClubIdAndUserId(clubId, requesterId);
        validateManagerPermission(requester);

        ClubMember target = clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId);

        if (target.isPresident()) {
            throw CustomException.of(CANNOT_DELETE_CLUB_PRESIDENT);
        }

        if (!requester.canManage(target)) {
            throw CustomException.of(CANNOT_MANAGE_HIGHER_POSITION);
        }

        if (target.getPositionGroup() != MEMBER) {
            throw CustomException.of(CANNOT_REMOVE_NON_MEMBER);
        }

        clubMemberRepository.delete(target);
    }

    private void validateNotSelf(Integer userId1, Integer userId2, ApiResponseCode errorCode) {
        if (userId1.equals(userId2)) {
            throw CustomException.of(errorCode);
        }
    }

    private void validatePresidentPermission(ClubMember member) {
        if (!member.isPresident()) {
            throw CustomException.of(FORBIDDEN_CLUB_MANAGER_ACCESS);
        }
    }

    private void validateManagerPermission(ClubMember member) {
        ClubPositionGroup positionGroup = member.getPositionGroup();
        if (positionGroup != PRESIDENT && positionGroup != VICE_PRESIDENT) {
            throw CustomException.of(FORBIDDEN_MEMBER_POSITION_CHANGE);
        }
    }
}
