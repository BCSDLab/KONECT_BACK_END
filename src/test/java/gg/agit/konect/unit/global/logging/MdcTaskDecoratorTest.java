package gg.agit.konect.unit.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import gg.agit.konect.global.logging.MdcTaskDecorator;

class MdcTaskDecoratorTest {

    @Test
    @DisplayName("task decorator는 호출 스레드의 MDC를 작업 스레드로 전달한다")
    void propagatesCallerMdcContext() {
        // given
        MdcTaskDecorator taskDecorator = new MdcTaskDecorator();
        AtomicReference<String> requestIdInTask = new AtomicReference<>();
        AtomicReference<String> traceIdInTask = new AtomicReference<>();
        MDC.put("requestId", "request-123");
        MDC.put("dd.trace_id", "trace-456");

        try {
            // when
            Runnable decoratedTask = taskDecorator.decorate(() -> {
                requestIdInTask.set(MDC.get("requestId"));
                traceIdInTask.set(MDC.get("dd.trace_id"));
            });
            MDC.clear();
            decoratedTask.run();

            // then
            assertThat(requestIdInTask.get()).isEqualTo("request-123");
            assertThat(traceIdInTask.get()).isEqualTo("trace-456");
            assertThat(MDC.getCopyOfContextMap()).isNull();
        } finally {
            MDC.clear();
        }
    }
}
