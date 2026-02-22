package gg.agit.konect.infrastructure.oauth;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.auth.oauth.OAuthTokenLoginRequest;
import gg.agit.konect.global.auth.oauth.OAuthTokenVerifier;
import gg.agit.konect.global.auth.oauth.VerifiedOAuthUser;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NaverTokenVerifier implements OAuthTokenVerifier {

    private static final String NAVER_USER_INFO_URI = "https://openapi.naver.com/v1/nid/me";

    private final RestTemplate restTemplate;

    @Override
    public Provider provider() {
        return Provider.NAVER;
    }

    @Override
    public VerifiedOAuthUser verify(OAuthTokenLoginRequest req) {
        String accessToken = req.accessToken();

        if (!StringUtils.hasText(accessToken)) {
            throw CustomException.of(ApiResponseCode.INVALID_OAUTH_TOKEN);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                NAVER_USER_INFO_URI,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {
                }
            );
            Map<String, Object> body = response.getBody();

            String providerId = extractProviderId(body);
            String email = extractEmail(body);

            if (!StringUtils.hasText(providerId)) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_PROVIDER_ID);
            }
            if (!StringUtils.hasText(email)) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
            }

            return new VerifiedOAuthUser(providerId, email);
        } catch (RestClientException e) {
            log.error(e.getMessage(), e);
            throw CustomException.of(ApiResponseCode.INVALID_OAUTH_TOKEN);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractProviderId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }

        Object rawResponse = body.get("response");

        if (!(rawResponse instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, Object> response = (Map<String, Object>)rawResponse;
        Object rawProviderId = response.get("id");

        if (!(rawProviderId instanceof String providerId)) {
            return null;
        }

        return providerId;
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(Map<String, Object> body) {
        if (body == null) {
            return null;
        }

        Object rawResponse = body.get("response");

        if (!(rawResponse instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, Object> response = (Map<String, Object>)rawResponse;
        Object rawEmail = response.get("email");

        if (!(rawEmail instanceof String email)) {
            return null;
        }

        return email;
    }
}
