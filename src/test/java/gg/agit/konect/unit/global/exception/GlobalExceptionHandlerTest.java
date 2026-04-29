package gg.agit.konect.unit.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import gg.agit.konect.global.exception.GlobalExceptionHandler;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final Logger exceptionHandlerLogger = (Logger)LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        originalLevel = exceptionHandlerLogger.getLevel();
        exceptionHandlerLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        exceptionHandlerLogger.setLevel(originalLevel);
        MDC.clear();
    }

    @Test
    @DisplayName("예상하지 못한 예외도 디버그 로그에서 요청 본문을 확인할 수 있다")
    void logsRequestBodyAtDebugLevelForUnexpectedException(CapturedOutput output) {
        // given
        MDC.put("requestId", "request-123");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/clubs");
        request.setContentType("application/json");
        request.addHeader("Authorization", "Bearer secret-token");
        request.addHeader("Cookie", "session=secret-cookie");
        request.addHeader("X-Request-ID", "request-1");
        request.setQueryString(
            "access%5Ftoken=encoded-query-secret&access_token=query-secret&code=oauth-code&name=KONECT"
                + "&token=repeat-secret-one&token=repeat-secret-two"
        );
        request.setContent("""
            {"name":"KONECT"}
            """.getBytes());
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // when
        var response = handler.handleException(wrappedRequest, new RuntimeException("boom"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(output)
            .contains("Request ID: `request-123`")
            .contains("Request [requestId: request-123]: POST /clubs")
            .contains("Authorization=***")
            .contains("Cookie=***")
            .contains("X-Request-ID=request-1")
            .doesNotContain("secret-token")
            .doesNotContain("secret-cookie")
            .contains(
                "Query String [requestId: request-123]: "
                    + "access%5Ftoken=***&access_token=***&code=***&name=KONECT&token=***&token=***"
            )
            .doesNotContain("encoded-query-secret")
            .doesNotContain("query-secret")
            .doesNotContain("oauth-code")
            .doesNotContain("repeat-secret-one")
            .doesNotContain("repeat-secret-two")
            .contains("Body [requestId: request-123]: {\"name\":\"KONECT\"}");
    }

    @Test
    @DisplayName("DEBUG 로그가 꺼져 있으면 요청 상세 정보를 계산하지 않는다")
    void skipsRequestDetailLoggingWhenDebugIsDisabled(CapturedOutput output) {
        // given
        exceptionHandlerLogger.setLevel(Level.INFO);
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/clubs");
        request.setContentType("application/json");
        request.setContent("""
            {"name":"KONECT"}
            """.getBytes());
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // when
        var response = handler.handleException(wrappedRequest, new RuntimeException("boom"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(output)
            .doesNotContain("Request [requestId:")
            .doesNotContain("Body: {\"name\":\"KONECT\"}");
    }
}
