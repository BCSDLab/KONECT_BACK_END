package gg.agit.konect.domain.notification.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import gg.agit.konect.domain.notification.dto.NotificationInboxResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationInboxSseService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));

        SseEmitter previous = emitters.put(userId, emitter);
        if (previous != null) {
            previous.complete();
        }

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void send(Integer userId, NotificationInboxResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("notification").data(notification));
        } catch (IOException | IllegalStateException e) {
            log.warn("SSE send failed: userId={}", userId, e);
            emitters.remove(userId, emitter);
            if (e instanceof IOException ioException) {
                try {
                    emitter.completeWithError(ioException);
                } catch (IllegalStateException completeException) {
                    log.warn("SSE emitter already completed while closing after send failure: userId={}", userId,
                        completeException);
                }
            }
        }
    }
}
