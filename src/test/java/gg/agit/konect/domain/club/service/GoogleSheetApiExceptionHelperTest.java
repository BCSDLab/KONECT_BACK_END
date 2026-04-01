package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;

class GoogleSheetApiExceptionHelperTest {

    @Test
    @DisplayName("classifies only permission-related 403 reasons as access denied")
    void isAccessDeniedReturnsTrueOnlyForPermissionReasons() {
        GoogleJsonResponseException permissionDenied =
            googleException(403, "insufficientPermissions");
        GoogleJsonResponseException accessDenied =
            googleException(403, "accessDenied");
        GoogleJsonResponseException forbidden =
            googleException(403, "forbidden");
        GoogleJsonResponseException quotaExceeded =
            googleException(403, "quotaExceeded");

        assertThat(GoogleSheetApiExceptionHelper.isAccessDenied(permissionDenied)).isTrue();
        assertThat(GoogleSheetApiExceptionHelper.isAccessDenied(accessDenied)).isTrue();
        assertThat(GoogleSheetApiExceptionHelper.isAccessDenied(forbidden)).isTrue();
        assertThat(GoogleSheetApiExceptionHelper.isAccessDenied(quotaExceeded)).isFalse();
    }

    @Test
    @DisplayName("returns false for 403 responses without a reason")
    void isAccessDeniedReturnsFalseWhenReasonIsMissing() {
        GoogleJsonError error = new GoogleJsonError();
        error.setCode(403);
        error.setErrors(List.of());

        HttpResponseException.Builder builder = new HttpResponseException.Builder(
            403,
            null,
            new HttpHeaders()
        );

        GoogleJsonResponseException exception = new GoogleJsonResponseException(builder, error);

        assertThat(GoogleSheetApiExceptionHelper.isAccessDenied(exception)).isFalse();
    }

    @Test
    @DisplayName("classifies auth-related 401 as auth failure")
    void isAuthFailureReturnsTrueForAuthReasons() {
        GoogleJsonResponseException authFailure =
            googleException(401, "authError");

        assertThat(GoogleSheetApiExceptionHelper.isAuthFailure(authFailure)).isTrue();
    }

    @Test
    @DisplayName("classifies 404 as not found")
    void isNotFoundReturnsTrueFor404() {
        GoogleJsonResponseException notFound =
            googleException(404, "notFound");

        assertThat(GoogleSheetApiExceptionHelper.isNotFound(notFound)).isTrue();
    }

    private GoogleJsonResponseException googleException(int statusCode, String reason) {
        GoogleJsonError.ErrorInfo errorInfo = new GoogleJsonError.ErrorInfo();
        errorInfo.setReason(reason);

        GoogleJsonError error = new GoogleJsonError();
        error.setCode(statusCode);
        error.setErrors(List.of(errorInfo));

        HttpResponseException.Builder builder = new HttpResponseException.Builder(
            statusCode,
            null,
            new HttpHeaders()
        );

        return new GoogleJsonResponseException(builder, error);
    }
}
