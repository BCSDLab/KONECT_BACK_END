package gg.agit.konect.infrastructure.oauth;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UserOAuthAccount;
import gg.agit.konect.domain.user.repository.UserOAuthAccountRepository;
import gg.agit.konect.global.auth.util.SecureTokenGenerator;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.infrastructure.googlesheets.GoogleSheetsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveOAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";
    private static final String STATE_KEY_PREFIX = "drive:oauth:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final String CALLBACK_PATH = "/auth/oauth/google/drive/callback";

    private static final DefaultRedisScript<String> GET_DEL_SCRIPT = new DefaultRedisScript<>(
        "local v = redis.call('GET', KEYS[1]); if v then redis.call('DEL', KEYS[1]); end; return v;",
        String.class
    );

    private final GoogleSheetsProperties googleSheetsProperties;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate redis;
    private final SecureTokenGenerator secureTokenGenerator;

    public String buildAuthorizationUrl(Integer userId) {
        String state = secureTokenGenerator.generate();
        redis.opsForValue().set(STATE_KEY_PREFIX + state, userId.toString(), STATE_TTL);

        String callbackUri = buildCallbackUri();

        return UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
            .queryParam("client_id", googleSheetsProperties.oauthClientId())
            .queryParam("redirect_uri", callbackUri)
            .queryParam("response_type", "code")
            .queryParam("scope", DRIVE_SCOPE)
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .queryParam("state", state)
            .build()
            .toUriString();
    }

    @Transactional
    public void exchangeAndSaveToken(String code, String state) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            log.warn("Drive OAuth callback received empty code or state.");
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        String stateKey = STATE_KEY_PREFIX + state;
        String userIdStr = redis.execute(GET_DEL_SCRIPT, List.of(stateKey));

        if (userIdStr == null || userIdStr.isBlank()) {
            log.warn("Invalid or expired Drive OAuth state. state={}", state);
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        Integer userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            throw CustomException.of(ApiResponseCode.INVALID_SESSION);
        }

        String refreshToken = requestRefreshToken(code);

        if (refreshToken == null) {
            handleMissingRefreshToken(userId);
            return;
        }

        persistGoogleRefreshToken(userId, refreshToken);
    }

    public boolean isDriveConnected(Integer userId) {
        return userOAuthAccountRepository
            .findByUserIdAndProvider(userId, Provider.GOOGLE)
            .map(account -> StringUtils.hasText(account.getGoogleDriveRefreshToken()))
            .orElse(false);
    }

    private void persistGoogleRefreshToken(Integer userId, String refreshToken) {
        UserOAuthAccount account = userOAuthAccountRepository
            .findByUserIdAndProvider(userId, Provider.GOOGLE)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_GOOGLE_DRIVE_AUTH));

        account.updateGoogleDriveRefreshToken(refreshToken);
        log.info("Google Drive refresh token saved. userId={}", userId);
    }

    private void handleMissingRefreshToken(Integer userId) {
        UserOAuthAccount existing = userOAuthAccountRepository
            .findByUserIdAndProvider(userId, Provider.GOOGLE)
            .orElse(null);

        if (existing != null && StringUtils.hasText(existing.getGoogleDriveRefreshToken())) {
            log.info("Re-authorization detected, keeping existing refresh token. userId={}", userId);
            return;
        }

        log.error("No refresh_token received and no existing token. userId={}", userId);
        throw CustomException.of(ApiResponseCode.FAILED_GOOGLE_DRIVE_AUTH);
    }

    private String requestRefreshToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleSheetsProperties.oauthClientId());
        params.add("client_secret", googleSheetsProperties.oauthClientSecret());
        params.add("redirect_uri", buildCallbackUri());
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response =
                restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Failed to exchange Drive OAuth code for tokens. status={}",
                    response.getStatusCode());
                throw CustomException.of(ApiResponseCode.FAILED_GOOGLE_DRIVE_AUTH);
            }

            return (String)response.getBody().get("refresh_token");

        } catch (RestClientException e) {
            log.error("RestClient error while exchanging Drive OAuth code. cause={}", e.getMessage(), e);
            throw CustomException.of(ApiResponseCode.FAILED_GOOGLE_DRIVE_AUTH);
        }
    }

    private String buildCallbackUri() {
        String base = googleSheetsProperties.oauthCallbackBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + CALLBACK_PATH;
    }
}
