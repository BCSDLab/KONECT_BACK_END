package com.example.konect.global.logging;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StopWatch;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private final ObjectProvider<PathMatcher> pathMatcherProvider;
    private final LoggingProperties properties;


    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {
        HttpServletRequest httpRequest = request;
        if (isIgnoredLoggingRequest(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        var cachedRequest = new ContentCachingRequestWrapper(request);
        var cachedResponse = new ContentCachingResponseWrapper(response);
        StopWatch stopWatch = new StopWatch();
        String requestId = getRequestId(httpRequest);
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();

        try {
            MDC.put(REQUEST_ID, requestId);
            stopWatch.start();
            log.info("request start [requestId: {}, uri: {} {}]", requestId, method, uri);
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            stopWatch.stop();
            log.info("request end [requestId: {}, uri: {} {}, time: {}ms, status: {}]",
                requestId, method, uri, stopWatch.getTotalTimeMillis(), cachedResponse.getStatus());
            MDC.clear();
            cachedResponse.copyBodyToResponse();
        }
    }

    private boolean isIgnoredLoggingRequest(HttpServletRequest httpRequest) {
        return CorsUtils.isPreFlightRequest(httpRequest) || isIgnoredUrl(httpRequest);
    }

    private boolean isIgnoredUrl(HttpServletRequest request) {
        PathMatcher pathMatcher = this.pathMatcherProvider.getIfAvailable();
        Objects.requireNonNull(pathMatcher);
        return properties.ignoredUrlPatterns().stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
    }

    private String getRequestId(HttpServletRequest httpRequest) {
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (ObjectUtils.isEmpty(requestId)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return requestId;
    }
}
