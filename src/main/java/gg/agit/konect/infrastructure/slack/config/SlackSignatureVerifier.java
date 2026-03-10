package gg.agit.konect.infrastructure.slack.config;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
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
    private static final String VERSION_PREFIX = VERSION + "=";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;
    private static final long MILLIS_TO_SECONDS = 1000L;
    private static final int HEX_RADIX = 16;

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

        // 서명 검증 (constant-time comparison)
        byte[] expectedHash = calculateSignatureBytes(timestamp, requestBody);
        if (expectedHash == null) {
            return false;
        }

        byte[] providedHash = parseSignature(signature);
        if (providedHash == null) {
            log.warn("Slack 서명 검증 실패: 서명 형식 오류");
            return false;
        }

        boolean isValid = MessageDigest.isEqual(expectedHash, providedHash);

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

    private byte[] calculateSignatureBytes(String timestamp, String requestBody) {
        String signingSecret = slackProperties.signingSecret();
        if (signingSecret == null || signingSecret.isBlank()) {
            log.error("Slack signing secret이 설정되지 않았습니다.");
            return null;
        }

        String baseString = VERSION + ":" + timestamp + ":" + requestBody;

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                signingSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
            );
            mac.init(secretKey);
            return mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Slack 서명 계산 실패", e);
            return null;
        }
    }

    private byte[] parseSignature(String signature) {
        if (!signature.startsWith(VERSION_PREFIX)) {
            return null;
        }

        String hexPart = signature.substring(VERSION_PREFIX.length());
        return hexToBytes(hexPart);
    }

    private byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            return null;
        }

        try {
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int index = i * 2;
                bytes[i] = (byte)Integer.parseInt(hex.substring(index, index + 2), HEX_RADIX);
            }
            return bytes;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
