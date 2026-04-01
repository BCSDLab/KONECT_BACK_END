package gg.agit.konect.domain.club.service;

import java.util.List;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;

final class GoogleApiTestUtils {

    private GoogleApiTestUtils() {}

    static GoogleJsonResponseException googleException(int statusCode, String reason) {
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

    static HttpResponseException httpResponseException(int statusCode) {
        return new HttpResponseException.Builder(
            statusCode,
            null,
            new HttpHeaders()
        ).build();
    }
}
