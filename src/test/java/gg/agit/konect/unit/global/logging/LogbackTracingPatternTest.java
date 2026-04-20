package gg.agit.konect.unit.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;

class LogbackTracingPatternTest {

    @Test
    @DisplayName("운영 로그 패턴은 Datadog canonical MDC 키만 사용한다")
    void usesDatadogCanonicalMdcKeys() throws Exception {
        // given
        String logbackConfig = Files.readString(Path.of("src/main/resources/logback-spring.xml"));

        // when & then
        List<String> patterns = tracePatterns(logbackConfig);
        assertThat(patterns).hasSize(3);
        assertThat(patterns).allSatisfy(pattern -> {
            assertThat(pattern).contains("%X{dd.trace_id:-}");
            assertThat(pattern).contains("%X{dd.span_id:-}");
            assertThat(pattern).contains("%X{requestId:-}");
            assertThat(pattern).doesNotContain("%X{trace_id:-}");
            assertThat(pattern).doesNotContain("%X{traceId:-}");
            assertThat(pattern).doesNotContain("%X{span_id:-}");
            assertThat(pattern).doesNotContain("%X{spanId:-}");
        });
    }

    @Test
    @DisplayName("로그 패턴은 trace, span, request id를 구분된 값으로 렌더링한다")
    void rendersTraceSectionWithCanonicalKeys() {
        // given
        String pattern = "[trace=%X{dd.trace_id:-} span=%X{dd.span_id:-} request=%X{requestId:-}] %msg%n";
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger("test");
        LoggingEvent loggingEvent = new LoggingEvent(
            LogbackTracingPatternTest.class.getName(),
            logger,
            Level.INFO,
            "test message",
            null,
            null
        );
        loggingEvent.setMDCPropertyMap(Map.of(
            "dd.trace_id", "123",
            "dd.span_id", "456",
            "requestId", "req-789"
        ));

        PatternLayout layout = new PatternLayout();
        layout.setContext(loggerContext);
        layout.setPattern(pattern);
        layout.start();

        // when
        String rendered = layout.doLayout(loggingEvent);

        // then
        assertThat(rendered).isEqualTo("[trace=123 span=456 request=req-789] test message" + System.lineSeparator());
    }

    private List<String> tracePatterns(String logbackConfig) throws Exception {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var documentBuilder = documentBuilderFactory.newDocumentBuilder();
        var document = documentBuilder.parse(new InputSource(new StringReader(logbackConfig)));
        var patternNodes = document.getElementsByTagName("pattern");

        return java.util.stream.IntStream.range(0, patternNodes.getLength())
            .mapToObj(index -> patternNodes.item(index).getTextContent().trim())
            .filter(pattern -> pattern.contains("trace="))
            .toList();
    }
}
