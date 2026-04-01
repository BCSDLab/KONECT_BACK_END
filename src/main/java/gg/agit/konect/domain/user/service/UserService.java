package gg.agit.konect.domain.user.service;

import static gg.agit.konect.domain.club.enums.ClubPosition.MANAGERS;
import static gg.agit.konect.domain.club.enums.ClubPosition.PRESIDENT;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_DELETE_CLUB_PRESIDENT;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
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
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.infrastructure.oauth.AppleTokenRevocationService;
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
    private final AppleTokenRevocationService appleTokenRevocationService;
    private final ChatRoomMembershipService chatRoomMembershipService;
    private final UserOAuthAccountService userOAuthAccountService;
    private final UserOAuthAccountRepository userOAuthAccountRepository;

    @Transactional
    public Integer signup(String email, String providerId, Provider provider, SignupRequest request) {
        if (provider == Provider.APPLE && !StringUtils.hasText(providerId)) {
            throw CustomException.of(ApiResponseCode.INVALID_REQUEST_BODY);
        }
        if (StringUtils.hasText(providerId)) {
            userOAuthAccountRepository.findUserByProviderAndProviderId(provider, providerId)
                .ifPresent(u -> {
                    throw CustomException.of(ApiResponseCode.ALREADY_REGISTERED_USER);
                });
        }
        userOAuthAccountRepository.findUserByOauthEmailAndProvider(email, provider)
            .ifPresent(u -> {
                throw CustomException.of(ApiResponseCode.ALREADY_REGISTERED_USER);
            });
        UnRegisteredUser tempUser = findUnregisteredUser(email, providerId, provider);
        University university = universityRepository.findById(request.universityId())
            .orElseThrow(() -> CustomException.of(ApiResponseCode.UNIVERSITY_NOT_FOUND));
        User newUser = User.of(
            university,
            tempUser,
            request.name(),
            request.studentNumber(),
            request.isMarketingAgreement(),
            "https://stage-static.koreatech.in/konect/User_02.png"
        );
        User savedUser = userRepository.save(newUser);
        userOAuthAccountService.linkPrimaryOAuthAccount(
            savedUser,
            provider,
            providerId,
            email,
            tempUser.getAppleRefreshToken()
        );
        joinPreMembers(savedUser, university.getId(), request.studentNumber(), request.name());
        sendWelcomeMessage(savedUser);
        unRegisteredUserRepository.delete(tempUser);
        applicationEventPublisher.publishEvent(
            UserRegisteredEvent.from(savedUser.getEmail(), provider.name())
        );
        return savedUser.getId();
    }

    private void sendWelcomeMessage(User newUser) {
        try {
            User operator = userRepository.findFirstByRoleAndDeletedAtIsNullOrderByIdAsc(UserRole.ADMIN)
                .orElse(null);
            if (operator == null) {
                return;
            }
            ChatRoom.validateIsNotSameParticipant(operator, newUser);
            ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(
                    operator.getId(),
                    newUser.getId(),
                    ChatType.DIRECT
                )
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.directOf()));
            LocalDateTime joinedAt = Objects.requireNonNull(
                chatRoom.getCreatedAt(),
                "chatRoom.createdAt must not be null"
            );
            chatRoomMembershipService.addDirectMembers(chatRoom, operator, newUser, joinedAt);
            ChatMessage chatMessage = chatMessageRepository.save(
                ChatMessage.of(chatRoom, operator, DEFAULT_WELCOME_MESSAGE)
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

    private void joinPreMembers(User user, Integer universityId, String studentNumber, String name) {
        List<ClubPreMember> preMembers =
            clubPreMemberRepository.findAllByUniversityIdAndStudentNumberAndName(
                universityId, studentNumber, name
            );
        if (preMembers.isEmpty()) {
            return;
        }
        for (ClubPreMember preMember : preMembers) {
            if (preMember.getClubPosition() == PRESIDENT) {
                replaceCurrentPresident(preMember.getClub().getId(), user.getId());
            }
            ClubMember clubMember = ClubMember.builder()
                .club(preMember.getClub())
                .user(user)
                .clubPosition(preMember.getClubPosition())
                .build();
            ClubMember savedMember = clubMemberRepository.save(clubMember);
            chatRoomMembershipService.addClubMember(savedMember);
        }
        clubPreMemberRepository.deleteAll(preMembers);
    }

    private void replaceCurrentPresident(Integer clubId, Integer newPresidentUserId) {
        clubMemberRepository.findPresidentByClubId(clubId)
            .filter(currentPresident -> !currentPresident.getId().getUserId().equals(newPresidentUserId))
            .ifPresent(currentPresident -> {
                clubMemberRepository.delete(currentPresident);
                chatRoomMembershipService.removeClubMember(clubId, currentPresident.getUser().getId());
            });
    }

    public UserInfoResponse getUserInfo(Integer userId) {
        User user = userRepository.getById(userId);
        List<ClubMember> clubMembers = clubMemberRepository.findAllByUserId(user.getId());
        boolean isClubManager = clubMembers.stream()
            .anyMatch(clubMember -> MANAGERS.contains(clubMember.getClubPosition()));
        int joinedClubCount = clubMembers.size();
        Long unreadCouncilNoticeCount = councilNoticeReadRepository.countUnreadNoticesByUserId(user.getId());
        Long studyTime = studyTimeQueryService.getTotalStudyTime(userId);
        return UserInfoResponse.from(user, joinedClubCount, studyTime, unreadCouncilNoticeCount, isClubManager);
    }

    @Transactional(readOnly = false)
    public void deleteUser(Integer userId) {
        User user = userRepository.getById(userId);
        validateNotClubPresident(userId);
        List<UserOAuthAccount> oauthAccounts = userOAuthAccountRepository.findAllByUserId(userId);
        for (UserOAuthAccount account : oauthAccounts) {
            if (account.getProvider() == Provider.APPLE && StringUtils.hasText(account.getAppleRefreshToken())) {
                appleTokenRevocationService.revoke(account.getAppleRefreshToken());
            }
        }
        user.withdraw(LocalDateTime.now());
        userRepository.save(user);
        UserOAuthAccount primaryAccount = oauthAccounts.isEmpty() ? null : oauthAccounts.get(0);
        String providerName = primaryAccount != null ? primaryAccount.getProvider().name() : "UNKNOWN";
        applicationEventPublisher.publishEvent(
            UserWithdrawnEvent.from(user.getEmail(), providerName)
        );
    }

    private void validateNotClubPresident(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findByUserId(userId);
        boolean isPresident = clubMembers.stream().anyMatch(ClubMember::isPresident);
        if (isPresident) {
            throw CustomException.of(CANNOT_DELETE_CLUB_PRESIDENT);
        }
    }
}
