package gg.agit.konect.global.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class PhoneNumberUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^0(1[0-9])(\\d{3,4})(\\d{4})$"
    );
    private static final String FORMATTED_PHONE_PATTERN = "^01[0-9]-\\d{3,4}-\\d{4}$";
    private static final int MIN_LENGTH_WITH_COUNTRY_CODE = 10;
    private static final int COUNTRY_CODE_LENGTH = 2;
    private static final int GROUP_PREFIX = 1;
    private static final int GROUP_MIDDLE = 2;
    private static final int GROUP_LAST = 3;

    private PhoneNumberUtils() {
    }

    public static String format(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return null;
        }

        String cleaned = phoneNumber.replaceAll("\\D", "");

        if (cleaned.startsWith("82") && cleaned.length() > MIN_LENGTH_WITH_COUNTRY_CODE) {
            cleaned = "0" + cleaned.substring(COUNTRY_CODE_LENGTH);
        }

        if (!cleaned.startsWith("0")) {
            cleaned = "0" + cleaned;
        }

        Matcher matcher = PHONE_PATTERN.matcher(cleaned);
        if (matcher.matches()) {
            String prefix = matcher.group(GROUP_PREFIX);
            String middle = matcher.group(GROUP_MIDDLE);
            String last = matcher.group(GROUP_LAST);
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
