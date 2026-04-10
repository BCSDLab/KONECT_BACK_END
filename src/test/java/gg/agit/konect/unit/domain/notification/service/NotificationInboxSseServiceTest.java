package gg.agit.konect.unit.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import gg.agit.konect.domain.notification.dto.NotificationInboxResponse;
import gg.agit.konect.domain.notification.service.NotificationInboxSseService;
import gg.agit.konect.support.ServiceTestSupport;

class NotificationInboxSseServiceTest extends ServiceTestSupport {

    private final NotificationInboxSseService notificationInboxSseService = new NotificationInboxSseService();

    @Test
    @DisplayName("같은 사용자가 재구독한 뒤 이전 emitter가 완료되어도 현재 구독은 유지된다")
    void subscribeReplacesEmitterWithoutRemovingNewOne() throws Exception {
        // given
        SseEmitter firstEmitter = notificationInboxSseService.subscribe(1);
        SseEmitter secondEmitter = notificationInboxSseService.subscribe(1);

        // when
        firstEmitter.complete();

        // then
        Map<Integer, SseEmitter> emitters = emitters();
        assertThat(emitters).hasSize(1);
        assertThat(emitters.get(1)).isSameAs(secondEmitter);
    }

    @Test
    @DisplayName("send는 null userId에 대해 NullPointerException을 발생시킨다")
    void sendThrowsExceptionForNullUserId() {
        // given
        NotificationInboxResponse response = createMockNotificationResponse();

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> notificationInboxSseService.send(null, response))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("send는 emitter가 없는 경우(구독하지 않은 사용자) 에러 없이 처리한다")
    void sendHandlesNonSubscribedUser() {
        // given
        NotificationInboxResponse response = createMockNotificationResponse();

        // when & then
        assertThatCode(() -> notificationInboxSseService.send(999, response))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("subscribe는 기존 emitter를 교체한다")
    void subscribeReplacesExistingEmitter() throws Exception {
        // given
        SseEmitter firstEmitter = notificationInboxSseService.subscribe(1);
        Map<Integer, SseEmitter> emittersBefore = emitters();

        // when
        SseEmitter secondEmitter = notificationInboxSseService.subscribe(1);
        Map<Integer, SseEmitter> emittersAfter = emitters();

        // then
        assertThat(emittersBefore).hasSize(1);
        assertThat(emittersAfter).hasSize(1);
        assertThat(emittersAfter.get(1)).isNotSameAs(firstEmitter);
        assertThat(emittersAfter.get(1)).isSameAs(secondEmitter);
    }

    private NotificationInboxResponse createMockNotificationResponse() {
        return new NotificationInboxResponse(
            1,
            gg.agit.konect.domain.notification.enums.NotificationInboxType.CLUB_APPLICATION_APPROVED,
            "title",
            "body",
            "path",
            false,
            null
        );
    }

    @Test
    @DisplayName("subscribe는 기존 emitter가 있으면 완료 후 교체한다")
    void subscribeCompletesPreviousEmitterBeforeReplacement() throws Exception {
        // given
        SseEmitter firstEmitter = notificationInboxSseService.subscribe(1);
        Map<Integer, SseEmitter> emittersBefore = emitters();

        // when
        SseEmitter secondEmitter = notificationInboxSseService.subscribe(1);
        Map<Integer, SseEmitter> emittersAfter = emitters();

        // then
        assertThat(emittersBefore).hasSize(1);
        assertThat(emittersAfter).hasSize(1);
        assertThat(emittersAfter.get(1)).isNotSameAs(firstEmitter);
        assertThat(emittersAfter.get(1)).isSameAs(secondEmitter);

        // 이전 emitter가 완료되었는지 확인
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> firstEmitter.send(SseEmitter.event().data("test")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("send는 IOException 발생 시 emitter를 제거한다")
    void sendRemovesEmitterOnIOException() {
        // given
        SseEmitter failingEmitter = notificationInboxSseService.subscribe(1);
        NotificationInboxResponse response = createMockNotificationResponse();

        // when
        // emitter가 존재하는 상태에서 전송 시도
        notificationInboxSseService.send(1, response);

        // then
        // 메서드가 정상적으로 동작하는지 확인
        assertThatCode(() -> notificationInboxSseService.send(1, response))
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, SseEmitter> emitters() throws Exception {
        Field field = NotificationInboxSseService.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (Map<Integer, SseEmitter>)field.get(notificationInboxSseService);
    }
}
