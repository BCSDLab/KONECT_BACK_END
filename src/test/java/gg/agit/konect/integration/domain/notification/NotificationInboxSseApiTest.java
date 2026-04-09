package gg.agit.konect.integration.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class NotificationInboxSseApiTest extends IntegrationTestSupport {

    private static final String NOTIFICATION_STREAM_ENDPOINT = "/notifications/inbox/stream";

    @BeforeEach
    void setUp() throws Exception {
        University university = persist(UniversityFixture.create());
        Integer userId = persist(UserFixture.createUser(university, "알림유저", "2021136001")).getId();
        clearPersistenceContext();
        mockLoginUser(userId);
    }

    @Nested
    @DisplayName("GET /notifications/inbox/stream - 알림 SSE 구독")
    class Subscribe {

        @Test
        @DisplayName("SSE 구독을 시작하고 초기 connect 이벤트를 내려준다")
        void subscribeSuccess() throws Exception {
            // when
            MvcResult result = mockMvc.perform(get(NOTIFICATION_STREAM_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

            // then
            String responseBody = result.getResponse().getContentAsString();
            assertThat(result.getResponse().getContentType())
                .contains("text/event-stream");
            assertThat(responseBody)
                .contains("event:connect")
                .contains("data:connected");
        }
    }
}
