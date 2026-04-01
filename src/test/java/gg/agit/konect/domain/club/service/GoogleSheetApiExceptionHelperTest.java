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
    @DisplayName("권한 관련 403 reason만 access denied로 분류한다")
    void isAccessDeniedReturnsTrueOnlyForPermissionReasons() {
        GoogleJsonResponseException permissionDenied =
            googleException(403, "insufficientPermissions");
        GoogleJsonResponseException quotaExceeded =
            googleException(403, "quotaExceeded");

        assertThat(GoogleSheetApiExceptionHelper.isAccessDenied(permissionDenied)).isTrue();
        assertThat(GoogleSheetApiExceptionHelper.isAccessDenied(quotaExceeded)).isFalse();
    }

    @Test
    @DisplayName("401 auth error는 auth failure로 분류한다")
    void isAuthFailureReturnsTrueForAuthReasons() {
        GoogleJsonResponseException authFailure =
            googleException(401, "authError");

        assertThat(GoogleSheetApiExceptionHelper.isAuthFailure(authFailure)).isTrue();
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
