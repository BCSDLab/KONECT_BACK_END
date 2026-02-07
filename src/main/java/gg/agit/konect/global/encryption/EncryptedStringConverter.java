package gg.agit.konect.global.encryption;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for automatic encryption/decryption of string attributes.
 * Encrypts strings when storing to database and decrypts when loading from database.
 *
 * Usage: @Convert(converter = EncryptedStringConverter.class)
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String>, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * Converts entity attribute (plaintext string) to database column value (encrypted string).
     *
     * @param attribute plaintext string from entity
     * @return encrypted string for database, or original value if null/empty
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (!StringUtils.hasText(attribute)) {
            return attribute;
        }

        EncryptionUtil encryptionUtil = applicationContext.getBean(EncryptionUtil.class);
        EncryptionProperties properties = applicationContext.getBean(EncryptionProperties.class);

        return encryptionUtil.encrypt(attribute, properties.getChatKey());
    }

    /**
     * Converts database column value (encrypted string) to entity attribute (plaintext string).
     * Uses tryDecrypt to handle migration scenarios where data might not be encrypted.
     *
     * @param dbData encrypted string from database
     * @return plaintext string for entity, or original value if null/empty
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return dbData;
        }

        EncryptionUtil encryptionUtil = applicationContext.getBean(EncryptionUtil.class);
        EncryptionProperties properties = applicationContext.getBean(EncryptionProperties.class);

        return encryptionUtil.tryDecrypt(dbData, properties.getChatKey());
    }
}
