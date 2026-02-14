package gg.agit.konect.domain.user.service;

import static gg.agit.konect.domain.club.enums.ClubPosition.MEMBER;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_DELETE_CLUB_PRESIDENT;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_DELETE_USER_WITH_UNPAID_FEE;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.chat.direct.model.ChatMessage;
import gg.agit.konect.domain.chat.direct.model.ChatRoom;
import gg.agit.konect.domain.chat.direct.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.direct.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.notice.repository.CouncilNoticeReadRepository;
import gg.agit.konect.domain.studytime.service.StudyTimeQueryService;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.university.repository.UniversityRepository;
import gg.agit.konect.domain.user.dto.SignupRequest;
import gg.agit.konect.domain.user.dto.UserInfoResponse;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.event.UserRegisteredEvent;
import gg.agit.konect.domain.user.event.UserWithdrawnEvent;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String DEFAULT_WELCOME_MESSAGE = "KONECT에 오신 것을 환영합니다. 궁금한 점이 있으면 언제든 문의해 주세요.";

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;
    private final UniversityRepository universityRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPreMemberRepository clubPreMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CouncilNoticeReadRepository councilNoticeReadRepository;
    private final StudyTimeQueryService studyTimeQueryService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Integer signup(String email, String providerId, Provider provider, SignupRequest request) {
        if (provider == Provider.APPLE && !StringUtils.hasText(providerId)) {
            throw CustomException.of(ApiResponseCode.INVALID_REQUEST_BODY);
        }

        if (StringUtils.hasText(providerId)) {
            userRepository.findByProviderIdAndProvider(providerId, provider)
                .ifPresent(u -> {
                    throw CustomException.of(ApiResponseCode.ALREADY_REGISTERED_USER);
                });
        }

        userRepository.findByEmailAndProvider(email, provider)
            .ifPresent(u -> {
                throw CustomException.of(ApiResponseCode.ALREADY_REGISTERED_USER);
            });

        UnRegisteredUser tempUser = findUnregisteredUser(email, providerId, provider);

        University university = universityRepository.findById(request.universityId())
            .orElseThrow(() -> CustomException.of(ApiResponseCode.UNIVERSITY_NOT_FOUND));

        validateStudentNumberDuplicationOnSignup(university.getId(), request.studentNumber());

        User newUser = User.of(
            university,
            tempUser,
            request.name(),
            request.studentNumber(),
            request.isMarketingAgreement(),
            "https://stage-static.koreatech.in/konect/User_02.png"
        );

        User savedUser = userRepository.save(newUser);

        joinPreMembers(savedUser, university.getId(), request.studentNumber(), request.name());

        sendWelcomeMessage(savedUser);

        unRegisteredUserRepository.delete(tempUser);

        applicationEventPublisher.publishEvent(UserRegisteredEvent.from(savedUser.getEmail()));
        return savedUser.getId();
    }

    // TODO 추후에 슈퍼 어드민을 만들어 학교가 확장되는 것을 고려해야 함
    private void sendWelcomeMessage(User newUser) {
        try {
            User operator = userRepository.findFirstByRoleOrderByIdAsc(UserRole.ADMIN)
                .orElse(null);

            if (operator == null) {
                return;
            }

            ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(operator.getId(), newUser.getId())
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.of(operator, newUser)));

            ChatMessage chatMessage = chatMessageRepository.save(
                ChatMessage.of(chatRoom, operator, newUser, DEFAULT_WELCOME_MESSAGE)
            );

            chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());
        } catch (Exception e) {
            log.warn("회원가입 환영 메시지 전송 실패. userId={}", newUser.getId(), e);
        }
    }

    private UnRegisteredUser findUnregisteredUser(String email, String providerId, Provider provider) {
        if (StringUtils.hasText(providerId)) {
            if (unRegisteredUserRepository.existsByProviderIdAndProvider(providerId, provider)) {
                return unRegisteredUserRepository.getByProviderIdAndProvider(providerId, provider);
            }
        }

        return unRegisteredUserRepository.getByEmailAndProvider(email, provider);
    }

    // TODO. 초기 회원 처리 완료 후 제거
    private void joinPreMembers(User user, Integer universityId, String studentNumber, String name) {
        List<ClubPreMember> preMembers =
            clubPreMemberRepository.findAllByUniversityIdAndStudentNumberAndName(
                universityId, studentNumber, name
            );

        if (preMembers.isEmpty()) {
            return;
        }

        for (ClubPreMember preMember : preMembers) {
            ClubMember clubMember = ClubMember.builder()
                .club(preMember.getClub())
                .user(user)
                .clubPosition(MEMBER)
                .isFeePaid(false)
                .build();

            clubMemberRepository.save(clubMember);
        }

        clubPreMemberRepository.deleteAll(preMembers);
    }

    public UserInfoResponse getUserInfo(Integer userId) {
        User user = userRepository.getById(userId);
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserId(user.getId());
        boolean isClubManager = clubMembers.stream().anyMatch(ClubMember::isPresident);
        int joinedClubCount = clubMembers.size();
        Long unreadCouncilNoticeCount = councilNoticeReadRepository.countUnreadNoticesByUserId(user.getId());
        Long studyTime = studyTimeQueryService.getTotalStudyTime(userId);

        return UserInfoResponse.from(user, joinedClubCount, studyTime, unreadCouncilNoticeCount, isClubManager);
    }

    private void validateStudentNumberDuplicationOnSignup(Integer universityId, String studentNumber) {
        boolean exists = userRepository.existsByUniversityIdAndStudentNumber(universityId, studentNumber);

        if (exists) {
            throw CustomException.of(ApiResponseCode.DUPLICATE_STUDENT_NUMBER);
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
