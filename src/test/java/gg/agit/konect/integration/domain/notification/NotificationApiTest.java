package gg.agit.konect.integration.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import gg.agit.konect.domain.notification.dto.NotificationTokenDeleteRequest;
import gg.agit.konect.domain.notification.dto.NotificationTokenRegisterRequest;
import gg.agit.konect.domain.notification.model.NotificationDeviceToken;
import gg.agit.konect.domain.notification.repository.NotificationDeviceTokenRepository;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class NotificationApiTest extends IntegrationTestSupport {

    private static final String REGISTERED_TOKEN = "ExpoPushToken[registered-token]";
    private static final String UPDATED_TOKEN = "ExpoPushToken[updated-token]";
    private static final String INVALID_TOKEN = "invalid-token";

    @Autowired
    private NotificationDeviceTokenRepository notificationDeviceTokenRepository;

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        University university = persist(UniversityFixture.create());
        user = persist(UserFixture.createUser(university, "알림유저", "2021136001"));
    }

    @Nested
    @DisplayName("GET /notifications/tokens - 내 알림 토큰 조회")
    class GetMyToken {

        @Test
        @DisplayName("등록된 알림 토큰을 조회한다")
        void getMyTokenSuccess() throws Exception {
            // given
            persist(NotificationDeviceToken.of(user, REGISTERED_TOKEN));
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
            performGet("/notifications/tokens")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(REGISTERED_TOKEN));
        }

        @Test
        @DisplayName("등록된 알림 토큰이 없으면 404를 반환한다")
        void getMyTokenWhenMissingFails() throws Exception {
            // given
            mockLoginUser(user.getId());

            // when & then
            performGet("/notifications/tokens")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_NOTIFICATION_TOKEN"));
        }
    }

    @Nested
    @DisplayName("POST /notifications/tokens - 알림 토큰 등록")
    class RegisterToken {

        @Test
        @DisplayName("알림 토큰을 새로 등록한다")
        void registerTokenSuccess() throws Exception {
            // given
            mockLoginUser(user.getId());

            // when & then
            performPost("/notifications/tokens", new NotificationTokenRegisterRequest(REGISTERED_TOKEN))
                .andExpect(status().isOk());

            clearPersistenceContext();
            assertThat(notificationDeviceTokenRepository.findByUserId(user.getId()))
                .isPresent()
                .get()
                .extracting(NotificationDeviceToken::getToken)
                .isEqualTo(REGISTERED_TOKEN);
        }

        @Test
        @DisplayName("이미 등록된 토큰이 있으면 새 토큰으로 갱신한다")
        void registerTokenUpdatesExistingToken() throws Exception {
            // given
            persist(NotificationDeviceToken.of(user, REGISTERED_TOKEN));
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
            performPost("/notifications/tokens", new NotificationTokenRegisterRequest(UPDATED_TOKEN))
                .andExpect(status().isOk());

            clearPersistenceContext();
            assertThat(notificationDeviceTokenRepository.findByUserId(user.getId()))
                .isPresent()
                .get()
                .extracting(NotificationDeviceToken::getToken)
                .isEqualTo(UPDATED_TOKEN);
        }

        @Test
        @DisplayName("유효하지 않은 Expo 토큰이면 400을 반환한다")
        void registerTokenWithInvalidTokenFails() throws Exception {
            // given
            mockLoginUser(user.getId());

            // when & then
            performPost("/notifications/tokens", new NotificationTokenRegisterRequest(INVALID_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_NOTIFICATION_TOKEN"));
        }
    }

    @Nested
    @DisplayName("DELETE /notifications/tokens - 알림 토큰 삭제")
    class DeleteToken {

        @Test
        @DisplayName("등록된 알림 토큰을 삭제한다")
        void deleteTokenSuccess() throws Exception {
            // given
            persist(NotificationDeviceToken.of(user, REGISTERED_TOKEN));
            clearPersistenceContext();
            mockLoginUser(user.getId());

            // when & then
            performDelete("/notifications/tokens", new NotificationTokenDeleteRequest(REGISTERED_TOKEN))
                .andExpect(status().isOk());

            clearPersistenceContext();
            assertThat(notificationDeviceTokenRepository.findByUserId(user.getId())).isEmpty();
        }
    }
}
