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
     * 전화번호를 010-XXXX-XXXX 형식으로 변환합니다.
     * 구글 시트에서 앞자리 0이 잘린 경우(10자리, 1로 시작)도 복구합니다.
     * 예) 1012345678  -> 010-1234-5678
     *     01012345678 -> 010-1234-5678
     *     010-1234-5678 -> 010-1234-5678
     */
    public static String format(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.trim().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return "";
        }

        // 구글 시트에서 앞자리 0이 잘린 경우 복구: 10자리이고 1로 시작
        if (digits.length() == 10 && digits.startsWith("1")) {
            digits = "0" + digits;
        }

        // 011자리: XXX-XXXX-XXXX
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }

        // 010자리(10자리 010 시작): XXX-XXX-XXXX
        if (digits.length() == 10 && digits.startsWith("010")) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }

        // 서울 지역번호 02: 02-XXXX-XXXX(10자리) or 02-XXX-XXXX(9자리)
        if (digits.startsWith("02")) {
            if (digits.length() == 10) {
                return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
            }
            if (digits.length() == 9) {
                return digits.substring(0, 2) + "-" + digits.substring(2, 5) + "-" + digits.substring(5);
            }
        }

        // 기타 10자리: XXX-XXX-XXXX
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }

        return digits;
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
