package gg.agit.konect.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @SuppressWarnings("unchecked")
    private Map<Integer, SseEmitter> emitters() throws Exception {
        Field field = NotificationInboxSseService.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (Map<Integer, SseEmitter>)field.get(notificationInboxSseService);
    }
}
