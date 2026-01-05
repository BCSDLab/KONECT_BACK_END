package gg.agit.konect.domain.user.service;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_DELETE_CLUB_PRESIDENT;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_DELETE_USER_WITH_UNPAID_FEE;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.notice.repository.CouncilNoticeReadRepository;
import gg.agit.konect.domain.studytime.service.StudyTimeQueryService;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.university.repository.UniversityRepository;
import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.dto.UserInfoResponse;
import gg.agit.konect.domain.user.dto.UserUpdateRequest;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.event.UserRegisteredEvent;
import gg.agit.konect.domain.user.event.UserWithdrawnEvent;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;
    private final UniversityRepository universityRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CouncilNoticeReadRepository councilNoticeReadRepository;
    private final StudyTimeQueryService studyTimeQueryService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Integer signup(String email, Provider provider, SignupRequest request) {
        userRepository.findByEmailAndProvider(email, provider)
            .ifPresent(u -> {
                throw CustomException.of(ApiResponseCode.ALREADY_REGISTERED_USER);
            });

        UnRegisteredUser tempUser = unRegisteredUserRepository
            .findByEmailAndProvider(email, provider)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_UNREGISTERED_USER));

        University university = universityRepository.findById(request.universityId())
            .orElseThrow(() -> CustomException.of(ApiResponseCode.UNIVERSITY_NOT_FOUND));

        validateStudentNumberDuplicationOnSignup(university.getId(), request.studentNumber());

        User newUser = User.builder()
            .university(university)
            .email(tempUser.getEmail())
            .name(request.name())
            .studentNumber(request.studentNumber())
            .provider(tempUser.getProvider())
            .isMarketingAgreement(request.isMarketingAgreement())
            .imageUrl("https://stage-static.koreatech.in/konect/User_02.png")
            .build();

        User savedUser = userRepository.save(newUser);

        unRegisteredUserRepository.delete(tempUser);

        applicationEventPublisher.publishEvent(UserRegisteredEvent.from(savedUser.getEmail()));
        return savedUser.getId();
    }

    public UserInfoResponse getUserInfo(Integer userId) {
        User user = userRepository.getById(userId);
        int joinedClubCount = clubMemberRepository.findAllByUserId(user.getId()).size();
        Long unreadCouncilNoticeCount = councilNoticeReadRepository.countUnreadNoticesByUserId(user.getId());
        Long studyTime = studyTimeQueryService.getTotalStudyTime(userId);

        return UserInfoResponse.from(user, joinedClubCount, studyTime, unreadCouncilNoticeCount);
    }

    @Transactional
    public void updateUserInfo(Integer userId, UserUpdateRequest request) {
        User user = userRepository.getById(userId);

        validateStudentNumberDuplication(user, request);
        validatePhoneNumberDuplication(user, request);

        user.updateInfo(request.name(), request.studentNumber(), request.phoneNumber());
    }

    private void validateStudentNumberDuplication(User user, UserUpdateRequest request) {
        if (user.hasSameStudentNumber(request.studentNumber())) {
            return;
        }

        boolean exists = userRepository.existsByUniversityIdAndStudentNumberAndIdNot(
            user.getUniversity().getId(), request.studentNumber(), user.getId()
        );

        if (exists) {
            throw CustomException.of(ApiResponseCode.DUPLICATE_STUDENT_NUMBER);
        }
    }

    private void validateStudentNumberDuplicationOnSignup(Integer universityId, String studentNumber) {
        boolean exists = userRepository.existsByUniversityIdAndStudentNumber(universityId, studentNumber);

        if (exists) {
            throw CustomException.of(ApiResponseCode.DUPLICATE_STUDENT_NUMBER);
        }
    }

    private void validatePhoneNumberDuplication(User user, UserUpdateRequest request) {
        String phoneNumber = request.phoneNumber();

        if (!StringUtils.hasText(phoneNumber) || user.hasSamePhoneNumber(phoneNumber)) {
            return;
        }

        boolean exists = userRepository.existsByPhoneNumberAndIdNot(phoneNumber, user.getId());

        if (exists) {
            throw CustomException.of(ApiResponseCode.DUPLICATE_PHONE_NUMBER);
        }
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.getById(userId);

        validateNotClubPresident(userId);
        validatePaidFees(userId);
        userRepository.delete(user);

        applicationEventPublisher.publishEvent(UserWithdrawnEvent.from(user.getEmail()));
    }

    private void validateNotClubPresident(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findByUserId(userId);
        boolean isPresident = clubMembers.stream().anyMatch(ClubMember::isPresident);
        if (isPresident) {
            throw CustomException.of(CANNOT_DELETE_CLUB_PRESIDENT);
        }
    }

    private void validatePaidFees(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findByUserId(userId);
        boolean hasUnpaidFee = clubMembers.stream()
            .anyMatch(ClubMember::hasUnpaidFee);

        if (hasUnpaidFee) {
            throw CustomException.of(CANNOT_DELETE_USER_WITH_UNPAID_FEE);
        }
    }
}
