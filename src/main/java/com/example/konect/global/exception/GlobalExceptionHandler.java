package com.example.konect.global.exception;

import java.time.DateTimeException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.konect.global.code.ApiResponseCode;
import com.example.konect.infrastructure.slack.client.SlackClient;
import com.example.konect.infrastructure.slack.model.SlackNotification;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final SlackClient slackClient;

    @Value("${slack.webhook.url}")
    private String slackUrl;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(
        HttpServletRequest request,
        IllegalArgumentException e
    ) {
        return buildErrorResponse(request, ApiResponseCode.ILLEGAL_ARGUMENT, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(
        HttpServletRequest request,
        IllegalStateException e
    ) {
        return buildErrorResponse(request, ApiResponseCode.ILLEGAL_STATE, e.getMessage());
    }

    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<Object> DateTimeException(
        HttpServletRequest request,
        DateTimeException e
    ) {
        return buildErrorResponse(request, ApiResponseCode.INVALID_DATE_TIME, e.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Object> handleUnsupportedOperationException(
        HttpServletRequest request,
        UnsupportedOperationException e
    ) {
        return buildErrorResponse(request, ApiResponseCode.UNSUPPORTED_OPERATION, e.getMessage());
    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Object> handleClientAbortException(
        HttpServletRequest request,
        ClientAbortException e
    ) {
        return buildErrorResponse(request, ApiResponseCode.CLIENT_ABORTED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(
        HttpServletRequest request,
        MethodArgumentTypeMismatchException e
    ) {
        return buildErrorResponse(request, ApiResponseCode.INVALID_TYPE_VALUE, e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleObjectOptimisticLockingFailureException(
        HttpServletRequest request,
        ObjectOptimisticLockingFailureException e
    ) {
        return buildErrorResponse(request, ApiResponseCode.OPTIMISTIC_LOCKING_FAILURE, e.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest webRequest
    ) {
        HttpServletRequest request = ((ServletWebRequest)webRequest).getRequest();
        ApiResponseCode errorCode = ApiResponseCode.INVALID_REQUEST_BODY;
        String errorTraceId = UUID.randomUUID().toString();

        List<ErrorResponse.FieldError> fieldErrors = getFieldErrors(ex);
        String firstErrorMessage = getFirstFieldErrorMessage(fieldErrors, errorCode.getMessage());

        ErrorResponse body = new ErrorResponse(
            errorCode.getHttpStatus().value(),
            errorCode.getCode(),
            firstErrorMessage,
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
        HttpServletRequest request = ((ServletWebRequest)webRequest).getRequest();
        return buildErrorResponse(request, ApiResponseCode.NO_HANDLER_FOUND, e.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
        MissingServletRequestParameterException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        HttpServletRequest req = ((ServletWebRequest)request).getRequest();
        return buildErrorResponse(req, ApiResponseCode.MISSING_REQUIRED_PARAMETER, ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        HttpServletRequest req = ((ServletWebRequest)request).getRequest();
        return buildErrorResponse(
            req,
            ApiResponseCode.INVALID_JSON_FORMAT,
            ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(HttpServletRequest request, Exception e) {
        sendSlackNotification(request, e);

        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "An internal server error occurred.");
        return ResponseEntity.internalServerError().body(response);
    }

    private void sendSlackNotification(HttpServletRequest request, Exception e) {
        StackTraceElement origin = e.getStackTrace()[0];

        String errorLocation = String.format(
            "%s:%d",
            origin.getFileName(),
            origin.getLineNumber()
        );

        String message = String.format(
            """
                    🚨 서버에서 에러가 발생했습니다! 🚨
                    URI: `%s %s`
                    Location: `*%s*`
                    Exception: `*%s*`
                    ```%s```
                """,
            request.getMethod(),
            request.getRequestURI(),
            errorLocation,
            e.getClass().getSimpleName(),
            e.getMessage()
        );

        SlackNotification slackNotification = SlackNotification.builder()
            .slackUrl(slackUrl)
            .text(message)
            .build();

        slackClient.sendMessage(slackNotification);
    }

    private ResponseEntity<Object> buildErrorResponse(
        HttpServletRequest request,
        ApiResponseCode errorCode,
        String errorMessage
    ) {
        String errorTraceId = UUID.randomUUID().toString();

        ErrorResponse response = new ErrorResponse(
            errorCode.getHttpStatus().value(),
            errorCode.getCode(),
            errorCode.getMessage(),
            errorTraceId
        );
        return ResponseEntity.status(response.status()).body(response);
    }

    private String getFirstFieldErrorMessage(List<ErrorResponse.FieldError> fields, String defaultMessage) {
        if (fields.isEmpty()) {
            return defaultMessage;
        }
        return fields.get(0).message();
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
