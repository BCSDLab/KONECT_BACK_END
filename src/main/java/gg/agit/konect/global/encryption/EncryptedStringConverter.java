package gg.agit.konect.global.encryption;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

/**
 * 문자열 필드의 자동 암호화/복호화를 위한 JPA AttributeConverter입니다.
 * DB 저장 시 암호화하고, 조회 시 복호화합니다.
 *
 * 사용 예시: @Convert(converter = EncryptedStringConverter.class)
 */
@Converter
@Component
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionUtil encryptionUtil;
    private final EncryptionProperties properties;

    /**
     * 엔티티 속성 값(평문 문자열)을 DB 컬럼 값(암호문 문자열)으로 변환합니다.
     *
     * @param attribute 엔티티의 평문 문자열
     * @return DB에 저장할 암호문 문자열, 또는 null/빈값이면 원본
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (!StringUtils.hasText(attribute)) {
            return attribute;
        }

        return encryptionUtil.encrypt(attribute, properties.getChatKey());
    }

    /**
     * DB 컬럼 값(암호문 문자열)을 엔티티 속성 값(평문 문자열)으로 변환합니다.
     * 마이그레이션 과정에서 평문 데이터가 섞여 있을 수 있어 tryDecrypt를 사용합니다.
     *
     * @param dbData DB에서 읽은 암호문 문자열
     * @return 엔티티에 주입할 평문 문자열, 또는 null/빈값이면 원본
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return dbData;
        }

        return encryptionUtil.tryDecrypt(dbData, properties.getChatKey());
    }
}
