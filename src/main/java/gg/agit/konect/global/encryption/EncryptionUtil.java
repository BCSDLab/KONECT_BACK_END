package gg.agit.konect.global.encryption;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EncryptionUtil {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int AES_KEY_SIZE_BITS = 256;
    private static final int AES_KEY_SIZE_BYTES = AES_KEY_SIZE_BITS / 8;

    private final EncryptionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 주어진 평문을 암호화합니다.
     * IV(12 bytes) + ciphertext + AuthTag(16 bytes)를 Base64로 인코딩하여 반환합니다.
     *
     * @param plaintext 암호화할 평문
     * @param key      Base64 인코딩된 AES-256 키
     * @return Base64 인코딩된 암호화 결과 (IV + ciphertext + tag)
     * @throws CustomException 암호화 실패 시
     */
    public String encrypt(String plaintext, String key) {
        if (!StringUtils.hasText(plaintext)) {
            throw new IllegalArgumentException("plaintext must not be empty");
        }
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("key must not be empty");
        }

        try {
            byte[] decodedKey = Base64.getDecoder().decode(key);
            if (decodedKey.length != AES_KEY_SIZE_BYTES) {
                throw new IllegalArgumentException(
                    "Key must be " + AES_KEY_SIZE_BYTES + " bytes (256-bit)");
            }

            // 랜덤 IV 생성
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            // GCM 파라미터 설정
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            // 암호화 수행
            SecretKeySpec keySpec = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintextBytes = plaintext.getBytes();
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // IV + ciphertext를 결합하여 Base64 인코딩
            byte[] encryptedData = new byte[IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, encryptedData, IV_LENGTH_BYTES, ciphertext.length);

            return Base64.getEncoder().encodeToString(encryptedData);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw CustomException.of(ApiResponseCode.UNEXPECTED_SERVER_ERROR);
        }
    }

    /**
     * Base64 인코딩된 암호문을 복호화합니다.
     *
     * @param ciphertext Base64 인코딩된 암호문 (IV + ciphertext + tag)
     * @param key       Base64 인코딩된 AES-256 키
     * @return 복호화된 평문
     * @throws CustomException 복호화 실패 시
     */
    public String decrypt(String ciphertext, String key) {
        if (!StringUtils.hasText(ciphertext)) {
            throw new IllegalArgumentException("ciphertext must not be empty");
        }
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("key must not be empty");
        }

        try {
            byte[] decodedKey = Base64.getDecoder().decode(key);
            if (decodedKey.length != AES_KEY_SIZE_BYTES) {
                throw new IllegalArgumentException(
                    "Key must be " + AES_KEY_SIZE_BYTES + " bytes (256-bit)");
            }

            // Base64 디코딩
            byte[] encryptedData = Base64.getDecoder().decode(ciphertext);

            if (encryptedData.length < IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid ciphertext: too short");
            }

            // IV 추출
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(encryptedData, 0, iv, 0, IV_LENGTH_BYTES);

            // 암호문 추출
            byte[] actualCiphertext = new byte[encryptedData.length - IV_LENGTH_BYTES];
            System.arraycopy(encryptedData, IV_LENGTH_BYTES, actualCiphertext, 0,
                actualCiphertext.length);

            // GCM 파라미터 설정
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            // 복호화 수행
            SecretKeySpec keySpec = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintextBytes = cipher.doFinal(actualCiphertext);
            return new String(plaintextBytes);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw CustomException.of(ApiResponseCode.UNEXPECTED_SERVER_ERROR);
        }
    }

    /**
     * 주어진 문자열을 복호화하되, 실패 시 원본 값을 반환합니다.
     * 마이그레이션 과정에서 암호화되지 않은 데이터를 처리하기 위해 사용합니다.
     *
     * @param value 복호화할 값 (null일 수 있음)
     * @param key   Base64 인코딩된 AES-256 키
     * @return 복호화된 값, 또는 복호화 실패 시 원본 값
     */
    public String tryDecrypt(String value, String key) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        try {
            return decrypt(value, key);
        } catch (Exception e) {
            // 복호화 실패 시 원본 값 반환 (이미 평문인 경우)
            return value;
        }
    }

    /**
     * AES-256 암호화 키를 생성합니다.
     * 생성된 키는 Base64로 인코딩되어 반환됩니다.
     *
     * @return Base64 인코딩된 AES-256 키
     */
    public String generateKey() {
        byte[] key = new byte[AES_KEY_SIZE_BYTES];
        secureRandom.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
