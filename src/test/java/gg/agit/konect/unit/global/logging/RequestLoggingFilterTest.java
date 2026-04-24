package gg.agit.konect.unit.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

import gg.agit.konect.global.logging.LoggingProperties;
import gg.agit.konect.global.logging.RequestLoggingFilter;

class RequestLoggingFilterTest {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Test
    @DisplayName("유효한 request id 헤더는 응답 헤더에도 그대로 내려준다")
    void echoesValidatedRequestIdHeader() throws ServletException, IOException {
        // given
        RequestLoggingFilter filter = createFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/notifications/inbox");
        request.addHeader(REQUEST_ID_HEADER, "incoming-request-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain(new NoOpServlet()));

        // then
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("incoming-request-id");
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 request id 헤더는 trim 후 응답 헤더에 내려준다")
    void trimsIncomingRequestIdHeader() throws ServletException, IOException {
        // given
        RequestLoggingFilter filter = createFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/notifications/inbox");
        request.addHeader(REQUEST_ID_HEADER, "  incoming-request-id  ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain(new NoOpServlet()));

        // then
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("incoming-request-id");
    }

    @Test
    @DisplayName("request id 헤더가 없으면 생성한 값을 응답 헤더로 내려준다")
    void generatesAndReturnsRequestIdHeader() throws ServletException, IOException {
        // given
        RequestLoggingFilter filter = createFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/notifications/inbox");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain(new NoOpServlet()));

        // then
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isNotBlank();
    }

    @Test
    @DisplayName("유효하지 않은 request id 헤더면 새 값을 생성해 응답 헤더에 내려준다")
    void generatesRequestIdForInvalidHeader() throws ServletException, IOException {
        // given
        RequestLoggingFilter filter = createFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/notifications/inbox");
        request.addHeader(REQUEST_ID_HEADER, "invalid request id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain(new NoOpServlet()));

        // then
        assertThat(response.getHeader(REQUEST_ID_HEADER))
            .isNotBlank()
            .isNotEqualTo("invalid request id")
            .matches("[A-Za-z0-9._-]{1,128}");
    }

    private RequestLoggingFilter createFilter() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("pathMatcher", new AntPathMatcher());
        ObjectProvider<PathMatcher> pathMatcherProvider = beanFactory.getBeanProvider(PathMatcher.class);
        return new RequestLoggingFilter(pathMatcherProvider, new LoggingProperties(List.of()));
    }

    private static class NoOpServlet extends HttpServlet {

        @Override
        protected void service(jakarta.servlet.http.HttpServletRequest req,
            jakarta.servlet.http.HttpServletResponse resp) {
            resp.setStatus(MockHttpServletResponse.SC_OK);
        }
    }
}
