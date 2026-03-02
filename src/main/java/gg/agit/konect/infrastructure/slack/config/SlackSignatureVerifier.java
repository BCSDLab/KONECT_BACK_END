package gg.agit.konect.infrastructure.slack.config;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String VERSION = "v0";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;
    private static final long MILLIS_TO_SECONDS = 1000L;
    private static final int BYTE_MASK = 0xff;

    private final SlackProperties slackProperties;

    public boolean isValidRequest(String timestamp, String signature, String requestBody) {
        if (timestamp == null || signature == null || requestBody == null) {
            log.warn("Slack 서명 검증 실패: 필수 헤더 누락");
            return false;
        }

        // 타임스탬프 검증 (5분 이내)
        if (!isTimestampValid(timestamp)) {
            log.warn("Slack 서명 검증 실패: 타임스탬프 만료");
            return false;
        }

        // 서명 검증
        String expectedSignature = calculateSignature(timestamp, requestBody);
        boolean isValid = signature.equals(expectedSignature);

        if (!isValid) {
            log.warn("Slack 서명 검증 실패: 서명 불일치");
        }

        return isValid;
    }

    private boolean isTimestampValid(String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / MILLIS_TO_SECONDS;
            return Math.abs(currentTime - requestTime) <= TIMESTAMP_TOLERANCE_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String calculateSignature(String timestamp, String requestBody) {
        String signingSecret = slackProperties.signingSecret();
        if (signingSecret == null || signingSecret.isBlank()) {
            log.error("Slack signing secret이 설정되지 않았습니다.");
            return "";
        }

        String baseString = VERSION + ":" + timestamp + ":" + requestBody;

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                signingSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return VERSION + "=" + bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Slack 서명 계산 실패", e);
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(BYTE_MASK & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
