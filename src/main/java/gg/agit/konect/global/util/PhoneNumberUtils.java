package gg.agit.konect.global.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class PhoneNumberUtils {

    private static final int COUNTRY_CODE_LENGTH = 3;
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(?:\\+82|0)?\\s*-?\\s*(1[0-9])\\s*-?\\s*(\\d{3,4})\\s*-?\\s*(\\d{4})$"
    );
    private static final String FORMATTED_PHONE_PATTERN = "^0\\d{2}-\\d{3,4}-\\d{4}$";
    private static final String KOREA_COUNTRY_CODE = "+82";

    private PhoneNumberUtils() {
    }

    public static String format(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return null;
        }

        String cleaned = phoneNumber.replaceAll("[\\s\\-()]", "");

        if (cleaned.startsWith(KOREA_COUNTRY_CODE)) {
            cleaned = "0" + cleaned.substring(COUNTRY_CODE_LENGTH);
        }

        Matcher matcher = PHONE_PATTERN.matcher(cleaned);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String middle = matcher.group(2);
            String last = matcher.group(COUNTRY_CODE_LENGTH);
            return String.format("0%s-%s-%s", prefix, middle, last);
        }

        return null;
    }

    public static boolean isValidFormat(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return true;
        }
        return phoneNumber.matches(FORMATTED_PHONE_PATTERN);
    }
}
