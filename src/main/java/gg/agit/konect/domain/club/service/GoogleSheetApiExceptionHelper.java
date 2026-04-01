package gg.agit.konect.domain.club.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

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

    public static CustomException accessDenied() {
        return CustomException.of(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS);
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
        if (exception instanceof GoogleJsonResponseException responseException) {
            return responseException.getStatusCode();
        }
        return -1;
    }
}
