package gg.agit.konect.domain.club.service;

import java.io.IOException;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public final class GoogleSheetApiExceptionHelper {

    private static final int HTTP_STATUS_FORBIDDEN = 403;
    private static final int HTTP_STATUS_NOT_FOUND = 404;

    private GoogleSheetApiExceptionHelper() {}

    public static boolean isAccessDenied(IOException exception) {
        return getStatusCode(exception) == HTTP_STATUS_FORBIDDEN;
    }

    public static boolean isNotFound(IOException exception) {
        return getStatusCode(exception) == HTTP_STATUS_NOT_FOUND;
    }

    public static CustomException accessDenied(String detail) {
        return CustomException.of(ApiResponseCode.FORBIDDEN_GOOGLE_SHEET_ACCESS, detail);
    }

    private static int getStatusCode(IOException exception) {
        if (exception instanceof GoogleJsonResponseException responseException) {
            return responseException.getStatusCode();
        }
        return -1;
    }
}
