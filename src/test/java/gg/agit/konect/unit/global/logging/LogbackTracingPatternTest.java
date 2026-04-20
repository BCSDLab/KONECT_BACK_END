package gg.agit.konect.unit.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogbackTracingPatternTest {

    @Test
    @DisplayName("운영 로그 패턴은 Datadog trace id와 span id MDC 키를 포함한다")
    void includesDatadogMdcKeys() throws IOException {
        // given
        String logbackConfig = Files.readString(Path.of("src/main/resources/logback-spring.xml"));

        // when & then
        assertThat(logbackConfig).contains("%X{dd.trace_id:-}");
        assertThat(logbackConfig).contains("%X{dd.span_id:-}");
    }
}
