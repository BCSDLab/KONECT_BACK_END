package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpResponseException;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public final class GoogleSheetApiExceptionHelper {

    private static final Set<String> ACCESS_DENIED_REASONS = Set.of(
        "accessDenied",
        "forbidden",
        "insufficientFilePermissions",
        "insufficientPermissions",
        "notAuthorized",
        "required"
    );
    private static final Set<String> AUTH_FAILURE_REASONS = Set.of(
        "authError",
        "invalidCredentials",
        "unauthorized"
    );
    private static final String INVALID_GRANT_ERROR = "invalid_grant";
    private static final Pattern ERROR_FIELD_PATTERN =
        Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_DESCRIPTION_PATTERN =
        Pattern.compile("\"error_description\"\\s*:\\s*\"([^\"]+)\"");
    private static final int HTTP_STATUS_BAD_REQUEST = 400;
    private static final int HTTP_STATUS_FORBIDDEN = 403;
    private static final int HTTP_STATUS_UNAUTHORIZED = 401;
    private static final int HTTP_STATUS_NOT_FOUND = 404;

    private GoogleSheetApiExceptionHelper() {}

    public static boolean isAccessDenied(IOException exception) {
        if (exception instanceof GoogleJsonResponseException responseException) {
            if (responseException.getStatusCode() != HTTP_STATUS_FORBIDDEN) {
                return false;
            }
            return hasReason(responseException, ACCESS_DENIED_REASONS);
        }
        return getStatusCode(exception) == HTTP_STATUS_FORBIDDEN;
    }

    public static boolean isAuthFailure(IOException exception) {
        if (exception instanceof GoogleJsonResponseException responseException) {
            if (responseException.getStatusCode() != HTTP_STATUS_UNAUTHORIZED) {
                return false;
            }
            return hasReason(responseException, AUTH_FAILURE_REASONS)
                || !hasKnownReasons(responseException);
        }
        return getStatusCode(exception) == HTTP_STATUS_UNAUTHORIZED;
    }

    public static boolean isNotFound(IOException exception) {
        return getStatusCode(exception) == HTTP_STATUS_NOT_FOUND;
    }

    public static boolean isInvalidGrant(IOException exception) {
        if (getStatusCode(exception) != HTTP_STATUS_BAD_REQUEST) {
            return false;
        }

        String content = getResponseContent(exception);
        if (content == null) {
            return false;
        }

        return content.toLowerCase().contains(INVALID_GRANT_ERROR);
    }

    public static CustomException accessDenied() {
        return CustomException.of(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS);
    }

    public static CustomException invalidGoogleDriveAuth(IOException exception) {
        return CustomException.of(
            ApiResponseCode.INVALID_GOOGLE_DRIVE_AUTH,
            extractClientDetail(exception)
        );
    }

    public static String extractDetail(IOException exception) {
        HttpResponseException responseException = findResponseException(exception);
        if (responseException != null) {
            String content = responseException.getContent();
            if (content != null && !content.isBlank()) {
                return "%d %s%n%s".formatted(
                    responseException.getStatusCode(),
                    defaultStatusText(responseException.getStatusCode()),
                    content
                );
            }

            String message = responseException.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }
        }
        return exception.getMessage();
    }

    private static String extractClientDetail(IOException exception) {
        if (isInvalidGrant(exception)) {
            return sanitizeInvalidGrantDetail(exception);
        }
        return extractDetail(exception);
    }

    private static boolean hasReason(
        GoogleJsonResponseException exception,
        Set<String> expectedReasons
    ) {
        return getReasons(exception).stream()
            .anyMatch(expectedReasons::contains);
    }

    private static boolean hasKnownReasons(GoogleJsonResponseException exception) {
        return !getReasons(exception).isEmpty();
    }

    private static List<String> getReasons(GoogleJsonResponseException exception) {
        if (exception.getDetails() == null || exception.getDetails().getErrors() == null) {
            return List.of();
        }
        return exception.getDetails().getErrors().stream()
            .map(GoogleJsonError.ErrorInfo::getReason)
            .filter(reason -> reason != null && !reason.isBlank())
            .toList();
    }

    private static int getStatusCode(IOException exception) {
        HttpResponseException responseException = findResponseException(exception);
        if (responseException != null) {
            return responseException.getStatusCode();
        }
        return -1;
    }

    private static String getResponseContent(IOException exception) {
        HttpResponseException responseException = findResponseException(exception);
        if (responseException == null) {
            return null;
        }

        String content = responseException.getContent();
        if (content != null && !content.isBlank()) {
            return content;
        }

        return responseException.getMessage();
    }

    private static HttpResponseException findResponseException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof GoogleJsonResponseException responseException) {
                return responseException;
            }
            if (current instanceof HttpResponseException responseException) {
                return responseException;
            }
            current = current.getCause();
        }
        return null;
    }

    private static String sanitizeInvalidGrantDetail(IOException exception) {
        String content = getResponseContent(exception);
        if (content == null) {
            return "%d %s%nerror=%s".formatted(
                HTTP_STATUS_BAD_REQUEST,
                defaultStatusText(HTTP_STATUS_BAD_REQUEST),
                INVALID_GRANT_ERROR
            );
        }

        String error = extractJsonField(content, ERROR_FIELD_PATTERN);
        String errorDescription = extractJsonField(content, ERROR_DESCRIPTION_PATTERN);

        StringBuilder detail = new StringBuilder()
            .append(HTTP_STATUS_BAD_REQUEST)
            .append(' ')
            .append(defaultStatusText(HTTP_STATUS_BAD_REQUEST));
        if (error != null) {
            detail.append(System.lineSeparator()).append("error=").append(error);
        }
        if (errorDescription != null) {
            detail.append(System.lineSeparator())
                .append("error_description=")
                .append(errorDescription);
        }

        if (error == null && errorDescription == null) {
            detail.append(System.lineSeparator()).append("error=").append(INVALID_GRANT_ERROR);
        }
        return detail.toString();
    }

    private static String extractJsonField(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    private static String defaultStatusText(int statusCode) {
        return switch (statusCode) {
            case HTTP_STATUS_BAD_REQUEST -> "Bad Request";
            case HTTP_STATUS_UNAUTHORIZED -> "Unauthorized";
            case HTTP_STATUS_FORBIDDEN -> "Forbidden";
            case HTTP_STATUS_NOT_FOUND -> "Not Found";
            default -> "HTTP Error";
        };
    }
}
