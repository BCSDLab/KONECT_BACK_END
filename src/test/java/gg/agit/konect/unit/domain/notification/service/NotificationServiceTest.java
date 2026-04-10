package gg.agit.konect.unit.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.chat.service.ChatPresenceService;
import gg.agit.konect.domain.notification.dto.NotificationInboxResponse;
import gg.agit.konect.domain.notification.dto.NotificationTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenRegisterRequest;
import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationDeviceToken;
import gg.agit.konect.domain.notification.model.NotificationInbox;
import gg.agit.konect.domain.notification.repository.NotificationDeviceTokenRepository;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.domain.notification.service.NotificationInboxService;
import gg.agit.konect.domain.notification.service.NotificationService;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.domain.notification.service.ExpoPushClient;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;

class NotificationServiceTest extends ServiceTestSupport {

    private static final String VALID_TOKEN = "ExpoPushToken[valid-token]";

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationDeviceTokenRepository notificationDeviceTokenRepository;

    @Mock
    private NotificationMuteSettingRepository notificationMuteSettingRepository;

    @Mock
    private ChatPresenceService chatPresenceService;

    @Mock
    private ExpoPushClient expoPushClient;

    @Mock
    private NotificationInboxService notificationInboxService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("registerToken은 기존 토큰이 없으면 새 엔티티를 저장한다")
    void registerTokenSavesNewTokenWhenMissing() {
        // given
        User user = createUser(1, "2021136001");
        NotificationTokenRegisterRequest request = new NotificationTokenRegisterRequest(VALID_TOKEN);
        given(userRepository.getById(1)).willReturn(user);
        given(notificationDeviceTokenRepository.findByUserId(1)).willReturn(Optional.empty());

        // when
        notificationService.registerToken(1, request);

        // then
        verify(notificationDeviceTokenRepository).save(argThat(token ->
            token.getUser().equals(user) && token.getToken().equals(VALID_TOKEN)
        ));
    }

    @Test
    @DisplayName("registerToken은 기존 토큰이 있으면 값을 갱신한다")
    void registerTokenUpdatesExistingToken() {
        // given
        User user = createUser(1, "2021136001");
        NotificationDeviceToken existingToken = NotificationDeviceToken.of(user, "ExpoPushToken[old-token]");
        given(userRepository.getById(1)).willReturn(user);
        given(notificationDeviceTokenRepository.findByUserId(1)).willReturn(Optional.of(existingToken));

        // when
        notificationService.registerToken(1, new NotificationTokenRegisterRequest(VALID_TOKEN));

        // then
        assertThat(existingToken.getToken()).isEqualTo(VALID_TOKEN);
        verify(notificationDeviceTokenRepository, never()).save(any(NotificationDeviceToken.class));
    }

    @Test
    @DisplayName("registerToken은 Expo 형식이 아닌 토큰을 거부한다")
    void registerTokenRejectsInvalidExpoToken() {
        // given
        User user = createUser(1, "2021136001");
        given(userRepository.getById(1)).willReturn(user);

        // when & then
        assertThatThrownBy(
            () -> notificationService.registerToken(1, new NotificationTokenRegisterRequest("bad-token"))
        )
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode())
                .isEqualTo(ApiResponseCode.INVALID_NOTIFICATION_TOKEN));
        verify(notificationDeviceTokenRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("deleteToken은 일치하는 토큰이 있을 때만 삭제한다")
    void deleteTokenDeletesOnlyMatchingToken() {
        // given
        User user = createUser(1, "2021136001");
        NotificationDeviceToken token = NotificationDeviceToken.of(user, VALID_TOKEN);
        given(notificationDeviceTokenRepository.findByUserIdAndToken(1, VALID_TOKEN)).willReturn(Optional.of(token));
        given(notificationDeviceTokenRepository.findByUserIdAndToken(1, "ExpoPushToken[missing]"))
            .willReturn(Optional.empty());

        // when
        notificationService.deleteToken(1, new NotificationTokenDeleteRequest(VALID_TOKEN));
        notificationService.deleteToken(1, new NotificationTokenDeleteRequest("ExpoPushToken[missing]"));

        // then
        verify(notificationDeviceTokenRepository).delete(token);
    }

    @Test
    @DisplayName("sendChatNotification은 사용자가 채팅방에 있으면 푸시를 생략한다")
    void sendChatNotificationSkipsWhenUserAlreadyInRoom() {
        // given
        given(chatPresenceService.isUserInChatRoom(7, 3)).willReturn(true);

        // when
        assertThatCode(() -> notificationService.sendChatNotification(3, 7, "보낸이", "메시지"))
            .doesNotThrowAnyException();

        // then
        verify(notificationDeviceTokenRepository, never()).findTokensByUserId(any());
        verify(expoPushClient, never()).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("sendChatNotification은 뮤트되지 않은 사용자에게 잘린 미리보기 푸시를 보낸다")
    void sendChatNotificationSendsTruncatedPreview() {
        // given
        String message = "😀".repeat(31);
        String expectedPreview = "😀".repeat(30) + "...";
        given(chatPresenceService.isUserInChatRoom(7, 3)).willReturn(false);
        given(
            notificationMuteSettingRepository.findByTargetTypeAndTargetIdAndUserId(
                NotificationTargetType.CHAT_ROOM,
                7,
                3
            )
        ).willReturn(Optional.empty());
        given(notificationDeviceTokenRepository.findTokensByUserId(3)).willReturn(List.of(VALID_TOKEN));

        // when
        notificationService.sendChatNotification(3, 7, "보낸이", message);

        // then
        verify(expoPushClient).sendNotification(
            eq(3),
            eq(List.of(VALID_TOKEN)),
            eq("보낸이"),
            eq(expectedPreview),
            eq(Map.of("path", "chats/7"))
        );
    }

    @Test
    @DisplayName("sendGroupChatNotification은 발신자·접속중·뮤트 사용자를 제외하고 전송한다")
    void sendGroupChatNotificationFiltersRecipientsBeforeBatchSend() {
        // given
        String message = "안녕하세요 여러분 반갑습니다. " +
            "이 메시지는 미리보기 길이를 초과하도록 충분히 깁니다!";
        given(chatPresenceService.findUsersInChatRoom(10, List.of(2, 3, 4, 5))).willReturn(Set.of(2));
        given(notificationMuteSettingRepository.findMutedUserIdsByTargetTypeAndTargetIdAndUserIds(
            NotificationTargetType.CHAT_ROOM,
            10,
            List.of(2, 3, 4, 5)
        )).willReturn(Set.of(3));
        given(notificationDeviceTokenRepository.findTokensByUserIds(List.of(4, 5)))
            .willReturn(List.of("ExpoPushToken[token-4]", "ExpoPushToken[token-5]"));

        // when
        notificationService.sendGroupChatNotification(
            10,
            1,
            "KONECT",
            "홍길동",
            message,
            List.of(1, 2, 3, 4, 5)
        );

        // then
        ArgumentCaptor<List<ExpoPushClient.ExpoPushMessage>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(expoPushClient).sendBatchNotifications(messagesCaptor.capture());
        List<ExpoPushClient.ExpoPushMessage> messages = messagesCaptor.getValue();
        assertThat(messages).hasSize(2);
        assertThat(messages)
            .extracting(ExpoPushClient.ExpoPushMessage::to)
            .containsExactly("ExpoPushToken[token-4]", "ExpoPushToken[token-5]");
        assertThat(messages)
            .extracting(ExpoPushClient.ExpoPushMessage::title)
            .containsOnly("KONECT");
        assertThat(messages)
            .extracting(ExpoPushClient.ExpoPushMessage::body)
            .allSatisfy(body -> {
                assertThat(body).startsWith("홍길동: ");
                assertThat(body).endsWith("...");
            });
        assertThat(messages)
            .extracting(ExpoPushClient.ExpoPushMessage::data)
            .containsOnly(Map.of("path", "chats/10"));
    }

    @Test
    @DisplayName("sendGroupChatNotification은 필터링 후 대상이 없으면 배치 전송을 생략한다")
    void sendGroupChatNotificationSkipsWhenNoRecipientsRemain() {
        // given
        given(chatPresenceService.findUsersInChatRoom(10, List.of(2, 3))).willReturn(Set.of(2));
        given(notificationMuteSettingRepository.findMutedUserIdsByTargetTypeAndTargetIdAndUserIds(
            NotificationTargetType.CHAT_ROOM,
            10,
            List.of(2, 3)
        )).willReturn(Set.of(3));

        // when
        assertThatCode(() -> notificationService.sendGroupChatNotification(
            10,
            1,
            "KONECT",
            "홍길동",
            "메시지",
            List.of(1, 2, 3)
        )).doesNotThrowAnyException();

        // then
        verify(notificationDeviceTokenRepository, never()).findTokensByUserIds(any());
        verify(expoPushClient, never()).sendBatchNotifications(any());
    }

    @Test
    @DisplayName("sendClubApplicationApprovedNotification은 인앱 알림, SSE, 푸시를 함께 보낸다")
    void sendClubApplicationApprovedNotificationSendsInboxSseAndPush() {
        // given
        User user = createUser(3, "2021136003");
        NotificationInbox inbox = NotificationInbox.of(
            user,
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "KONECT",
            "동아리 지원이 승인되었어요.",
            "clubs/7"
        );
        given(notificationInboxService.save(
            3,
            NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "KONECT",
            "동아리 지원이 승인되었어요.",
            "clubs/7"
        )).willReturn(inbox);
        given(notificationDeviceTokenRepository.findTokensByUserId(3)).willReturn(List.of(VALID_TOKEN));

        // when
        notificationService.sendClubApplicationApprovedNotification(3, 7, "KONECT");

        // then
        ArgumentCaptor<NotificationInboxResponse> responseCaptor =
            ArgumentCaptor.forClass(NotificationInboxResponse.class);
        verify(notificationInboxService).sendSse(eq(3), responseCaptor.capture());
        NotificationInboxResponse response = responseCaptor.getValue();
        assertThat(response.type()).isEqualTo(NotificationInboxType.CLUB_APPLICATION_APPROVED);
        assertThat(response.title()).isEqualTo("KONECT");
        assertThat(response.body()).isEqualTo("동아리 지원이 승인되었어요.");
        assertThat(response.path()).isEqualTo("clubs/7");
        verify(expoPushClient).sendNotification(
            eq(3),
            eq(List.of(VALID_TOKEN)),
            eq("KONECT"),
            eq("동아리 지원이 승인되었어요."),
            eq(Map.of("path", "clubs/7"))
        );
    }

    private User createUser(Integer id, String studentNumber) {
        return User.builder()
            .id(id)
            .university(UniversityFixture.create())
            .email(studentNumber + "@koreatech.ac.kr")
            .name("테스트유저" + id)
            .studentNumber(studentNumber)
            .isMarketingAgreement(true)
            .imageUrl("https://example.com/profile-" + id + ".png")
            .build();
    }
}
