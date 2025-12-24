package gg.agit.konect.global.exception;

import java.time.DateTimeException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import gg.agit.konect.global.code.ApiResponseCode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(
        HttpServletRequest request,
        CustomException e
    ) {
        return buildErrorResponse(request, e.getErrorCode(), e.getFullMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException() {
        return buildErrorResponse(ApiResponseCode.ILLEGAL_ARGUMENT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException() {
        return buildErrorResponse(ApiResponseCode.ILLEGAL_STATE);
    }

    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<Object> dateTimeException() {
        return buildErrorResponse(ApiResponseCode.INVALID_DATE_TIME);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Object> handleUnsupportedOperationException() {
        return buildErrorResponse(ApiResponseCode.UNSUPPORTED_OPERATION);
    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Object> handleClientAbortException() {
        return buildErrorResponse(ApiResponseCode.CLIENT_ABORTED);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException() {
        return buildErrorResponse(ApiResponseCode.INVALID_TYPE_VALUE);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleObjectOptimisticLockingFailureException() {
        return buildErrorResponse(ApiResponseCode.OPTIMISTIC_LOCKING_FAILURE);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return buildErrorResponse(ApiResponseCode.METHOD_NOT_ALLOWED);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest webRequest
    ) {
        ApiResponseCode errorCode = ApiResponseCode.INVALID_REQUEST_BODY;
        String errorTraceId = UUID.randomUUID().toString();

        List<ErrorResponse.FieldError> fieldErrors = getFieldErrors(ex);

        ErrorResponse body = new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            errorTraceId,
            fieldErrors
        );

        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @Override
    public ResponseEntity<Object> handleNoHandlerFoundException(
        NoHandlerFoundException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest webRequest
    ) {
        return buildErrorResponse(ApiResponseCode.NO_HANDLER_FOUND);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
        MissingServletRequestParameterException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return buildErrorResponse(ApiResponseCode.MISSING_REQUIRED_PARAMETER);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return buildErrorResponse(ApiResponseCode.INVALID_JSON_FORMAT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(HttpServletRequest request, Exception e) {
        StackTraceElement origin = e.getStackTrace()[0];

        String uri = String.format("%s %s", request.getMethod(), request.getRequestURI());
        String location = String.format(
            "%s:%d",
            origin.getFileName(),
            origin.getLineNumber()
        );
        String exception = e.getClass().getSimpleName();
        String message = e.getMessage();

        MDC.put("uri", uri);
        MDC.put("location", location);
        MDC.put("exception", exception);
        MDC.put("message", message);

        log.error("URI: {} | Location: {} | Exception: {} | Message: {}", uri, location, exception, message);

        return buildErrorResponse(ApiResponseCode.UNEXPECTED_SERVER_ERROR);
    }

    private ResponseEntity<Object> buildErrorResponse(ApiResponseCode errorCode) {
        String errorTraceId = UUID.randomUUID().toString();

        ErrorResponse response = new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            errorTraceId
        );

        return ResponseEntity.status(errorCode.getHttpStatus().value()).body(response);
    }

    private ResponseEntity<Object> buildErrorResponse(
        HttpServletRequest request,
        ApiResponseCode errorCode,
        String errorMessage
    ) {
        String errorTraceId = UUID.randomUUID().toString();
        requestLogging(request, errorCode.getHttpStatus().value(), errorMessage, errorTraceId);

        ErrorResponse response = new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            errorTraceId
        );

        return ResponseEntity.status(errorCode.getHttpStatus().value()).body(response);
    }

    private void requestLogging(
        HttpServletRequest request,
        int httpStatus,
        String errorMessage,
        String errorTraceId
    ) {
        log.warn("[{}] {} | errorTraceId={}", httpStatus, errorMessage, errorTraceId);
        log.debug("Request: {} {}", request.getMethod(), request.getRequestURI());
        log.debug("Headers: {}", getHeaders(request));
        log.debug("Query String: {}", getQueryString(request));
        log.debug("Body: {}", getRequestBody(request));
    }

    private Map<String, Object> getHeaders(HttpServletRequest request) {
        Map<String, Object> headerMap = new HashMap<>();
        Enumeration<String> headerArray = request.getHeaderNames();
        while (headerArray.hasMoreElements()) {
            String headerName = headerArray.nextElement();
            headerMap.put(headerName, request.getHeader(headerName));
        }
        return headerMap;
    }

    private String getQueryString(HttpServletRequest httpRequest) {
        String queryString = httpRequest.getQueryString();
        if (queryString == null) {
            return " - ";
        }
        return queryString;
    }

    private String getRequestBody(HttpServletRequest request) {
        var wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper == null) {
            return " - ";
        }
        try {
            // body 가 읽히지 않고 예외처리 되는 경우에 캐시하기 위함
            wrapper.getInputStream().readAllBytes();
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length == 0) {
                return " - ";
            }
            return new String(buf, wrapper.getCharacterEncoding());
        } catch (Exception e) {
            return " - ";
        }
    }

    private List<ErrorResponse.FieldError> getFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldError)
            .toList();
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        String field = fe.getField();
        String constraint = Objects.requireNonNull(fe.getCode());
        String message = Objects.requireNonNullElse(
            fe.getDefaultMessage(), ApiResponseCode.INVALID_REQUEST_BODY.getMessage()
        );

        return new ErrorResponse.FieldError(field, message, constraint);
    }
}
