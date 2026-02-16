package gg.agit.konect.infrastructure.oauth;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleTokenRevocationService {

    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";

    private final RestTemplate restTemplate;
    private final AppleClientSecretProvider appleClientSecretProvider;
    private final AppleOAuthProperties appleOAuthProperties;

    public void revoke(String appleRefreshToken) {
        if (!StringUtils.hasText(appleRefreshToken)) {
            log.warn("Apple refresh token이 없어 revoke를 건너뜁니다.");
            return;
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", appleOAuthProperties.getClientId());
        params.add("client_secret", appleClientSecretProvider.getClientSecret());
        params.add("token", appleRefreshToken);
        params.add("token_type_hint", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(APPLE_REVOKE_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Apple 토큰 revoke 완료");
            }
        } catch (HttpClientErrorException e) {
            log.error("Apple 토큰 revoke 실패. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Apple 토큰 revoke에 실패했습니다.", e);
        }
    }
}
