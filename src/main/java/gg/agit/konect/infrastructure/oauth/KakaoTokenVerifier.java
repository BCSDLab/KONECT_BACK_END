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
public class KakaoTokenVerifier implements OAuthTokenVerifier {

    private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestTemplate restTemplate;

    @Override
    public Provider provider() {
        return Provider.KAKAO;
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
                KAKAO_USER_INFO_URI,
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

            return new VerifiedOAuthUser(providerId, email, null);
        } catch (RestClientException e) {
            log.error(e.getMessage(), e);
            throw CustomException.of(ApiResponseCode.INVALID_OAUTH_TOKEN);
        }
    }

    private String extractProviderId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }

        Object rawProviderId = body.get("id");
        return rawProviderId == null ? null : String.valueOf(rawProviderId);
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(Map<String, Object> body) {
        if (body == null) {
            return null;
        }

        Object rawKakaoAccount = body.get("kakao_account");

        if (!(rawKakaoAccount instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, Object> kakaoAccount = (Map<String, Object>)rawKakaoAccount;
        Object rawEmail = kakaoAccount.get("email");

        if (!(rawEmail instanceof String email)) {
            return null;
        }

        return email;
    }
}
