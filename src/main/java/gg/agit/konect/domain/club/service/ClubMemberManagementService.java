package gg.agit.konect.domain.club.service;

import static gg.agit.konect.domain.club.enums.ClubPosition.*;
import static gg.agit.konect.global.code.ApiResponseCode.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubPreMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubPreMemberAddResponse;
import gg.agit.konect.domain.club.dto.MemberPositionChangeRequest;
import gg.agit.konect.domain.club.dto.PresidentTransferRequest;
import gg.agit.konect.domain.club.dto.VicePresidentChangeRequest;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
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
    private final ClubPreMemberRepository clubPreMemberRepository;
    private final ClubPermissionValidator clubPermissionValidator;
    private final UserRepository userRepository;

    @Transactional
    public ClubMember changeMemberPosition(
        Integer clubId,
        Integer targetUserId,
        Integer requesterId,
        MemberPositionChangeRequest request
    ) {
        clubRepository.getById(clubId);

        validateNotSelf(requesterId, targetUserId, CANNOT_CHANGE_OWN_POSITION);

        clubPermissionValidator.validateLeaderAccess(clubId, requesterId);

        ClubMember requester = clubMemberRepository.getByClubIdAndUserId(clubId, requesterId);
        ClubMember target = clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId);

        if (!requester.canManage(target)) {
            throw CustomException.of(CANNOT_MANAGE_HIGHER_POSITION);
        }

        ClubPosition newPosition = request.position();

        if (!requester.getClubPosition().canManage(newPosition)) {
            throw CustomException.of(FORBIDDEN_MEMBER_POSITION_CHANGE);
        }

        validatePositionLimit(clubId, newPosition, target);

        target.changePosition(newPosition);

        return target;
    }

    @Transactional
    public ClubPreMemberAddResponse addPreMember(
        Integer clubId,
        Integer requesterId,
        ClubPreMemberAddRequest request
    ) {
        Club club = clubRepository.getById(clubId);

        clubPermissionValidator.validateLeaderAccess(clubId, requesterId);

        String studentNumber = request.studentNumber();
        String name = request.name();
        Integer universityId = club.getUniversity().getId();

        Optional<User> existingUser = userRepository.findByUniversityIdAndStudentNumber(
            universityId, studentNumber
        );

        if (existingUser.isPresent()) {
            return addDirectMember(club, existingUser.get());
        }

        return addPreMemberInternal(club, studentNumber, name);
    }

    private ClubPreMemberAddResponse addDirectMember(Club club, User user) {
        Integer clubId = club.getId();
        Integer userId = user.getId();

        if (clubMemberRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw CustomException.of(ALREADY_CLUB_MEMBER);
        }

        ClubMember clubMember = ClubMember.builder()
            .club(club)
            .user(user)
            .clubPosition(MEMBER)
            .isFeePaid(false)
            .build();

        ClubMember savedMember = clubMemberRepository.save(clubMember);
        return ClubPreMemberAddResponse.from(savedMember);
    }

    private ClubPreMemberAddResponse addPreMemberInternal(Club club, String studentNumber, String name) {
        Integer clubId = club.getId();

        if (clubPreMemberRepository.existsByClubIdAndStudentNumberAndName(clubId, studentNumber, name)) {
            throw CustomException.of(ALREADY_CLUB_PRE_MEMBER);
        }

        ClubPreMember preMember = ClubPreMember.builder()
            .club(club)
            .studentNumber(studentNumber)
            .name(name)
            .build();

        ClubPreMember savedPreMember = clubPreMemberRepository.save(preMember);
        return ClubPreMemberAddResponse.from(savedPreMember);
    }

    @Transactional
    public List<ClubMember> transferPresident(
        Integer clubId,
        Integer currentPresidentId,
        PresidentTransferRequest request
    ) {
        clubRepository.getById(clubId);

        clubPermissionValidator.validatePresidentAccess(clubId, currentPresidentId);

        Integer newPresidentUserId = request.newPresidentUserId();

        ClubMember currentPresident = clubMemberRepository.getByClubIdAndUserId(clubId, currentPresidentId);
        validateNotSelf(currentPresidentId, newPresidentUserId, ILLEGAL_ARGUMENT);

        ClubMember newPresident = clubMemberRepository.getByClubIdAndUserId(clubId, newPresidentUserId);

        currentPresident.changePosition(MEMBER);
        newPresident.changePosition(PRESIDENT);

        return List.of(currentPresident, newPresident);
    }

    @Transactional
    public List<ClubMember> changeVicePresident(
        Integer clubId,
        Integer requesterId,
        VicePresidentChangeRequest request
    ) {
        clubRepository.getById(clubId);

        clubPermissionValidator.validatePresidentAccess(clubId, requesterId);

        Optional<ClubMember> currentVicePresidentOpt = clubMemberRepository.findAllByClubIdAndPosition(
                clubId,
                VICE_PRESIDENT
            )
            .stream()
            .findFirst();

        Integer newVicePresidentUserId = request.vicePresidentUserId();
        List<ClubMember> changedMembers = new ArrayList<>();

        if (newVicePresidentUserId == null) {
            if (currentVicePresidentOpt.isPresent()) {
                ClubMember currentVicePresident = currentVicePresidentOpt.get();
                currentVicePresident.changePosition(MEMBER);
                changedMembers.add(currentVicePresident);
            }
            return changedMembers;
        }

        validateNotSelf(requesterId, newVicePresidentUserId, CANNOT_CHANGE_OWN_POSITION);

        ClubMember newVicePresident = clubMemberRepository.getByClubIdAndUserId(clubId, newVicePresidentUserId);

        if (currentVicePresidentOpt.isPresent()) {
            ClubMember currentVicePresident = currentVicePresidentOpt.get();
            if (!currentVicePresident.getId().getUserId().equals(newVicePresidentUserId)) {
                currentVicePresident.changePosition(MEMBER);
                changedMembers.add(currentVicePresident);
            }
        }

        newVicePresident.changePosition(VICE_PRESIDENT);
        changedMembers.add(newVicePresident);

        return changedMembers;
    }

    @Transactional
    public void removeMember(Integer clubId, Integer targetUserId, Integer requesterId) {
        clubRepository.getById(clubId);

        validateNotSelf(requesterId, targetUserId, CANNOT_REMOVE_SELF);

        clubPermissionValidator.validateLeaderAccess(clubId, requesterId);

        ClubMember requester = clubMemberRepository.getByClubIdAndUserId(clubId, requesterId);
        ClubMember target = clubMemberRepository.getByClubIdAndUserId(clubId, targetUserId);

        if (target.isPresident()) {
            throw CustomException.of(CANNOT_DELETE_CLUB_PRESIDENT);
        }

        if (!requester.canManage(target)) {
            throw CustomException.of(CANNOT_MANAGE_HIGHER_POSITION);
        }

        if (target.getClubPosition() != MEMBER) {
            throw CustomException.of(CANNOT_REMOVE_NON_MEMBER);
        }

        clubMemberRepository.delete(target);
    }

    private void validateNotSelf(Integer userId1, Integer userId2, ApiResponseCode errorCode) {
        if (userId1.equals(userId2)) {
            throw CustomException.of(errorCode);
        }
    }

    private void validatePositionLimit(Integer clubId, ClubPosition newPosition, ClubMember existingMember) {
        if (newPosition == VICE_PRESIDENT) {
            boolean isAlreadyVicePresident = existingMember != null
                && existingMember.getClubPosition() == VICE_PRESIDENT;
            if (isAlreadyVicePresident) {
                return;
            }
            long count = clubMemberRepository.countByClubIdAndPosition(clubId, VICE_PRESIDENT);
            if (count >= 1) {
                throw CustomException.of(VICE_PRESIDENT_ALREADY_EXISTS);
            }
        }

        if (newPosition == MANAGER) {
            boolean isAlreadyManager = existingMember != null
                && existingMember.getClubPosition() == MANAGER;
            if (isAlreadyManager) {
                return;
            }
            long count = clubMemberRepository.countByClubIdAndPosition(clubId, MANAGER);
            if (count >= MAX_MANAGER_COUNT) {
                throw CustomException.of(MANAGER_LIMIT_EXCEEDED);
            }
        }
    }
}
