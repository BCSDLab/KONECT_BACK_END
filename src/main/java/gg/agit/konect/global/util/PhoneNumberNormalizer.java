package gg.agit.konect.global.util;

public final class PhoneNumberNormalizer {

    private static final int PHONE_NUMBER_MIN_DIGITS = 9;
    private static final int PHONE_NUMBER_MAX_DIGITS = 11;

    private PhoneNumberNormalizer() {
    }

    /**
     * 다양한 형식의 전화번호 문자열을 숫자만 남긴 형태로 정규화합니다.
     * 예) 010-1234-5678 -> 01012345678
     *     (010) 1234-5678 -> 01012345678
     *     010 1234 5678  -> 01012345678
     */
    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceAll("[^0-9]", "");
    }

    /**
     * 전화번호처럼 보이는지 간단히 검증합니다.
     * 숫자만 남겼을 때 9~11자리면 유효한 것으로 판단합니다.
     */
    public static boolean looksLikePhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() >= PHONE_NUMBER_MIN_DIGITS
            && digits.length() <= PHONE_NUMBER_MAX_DIGITS;
    }
}
