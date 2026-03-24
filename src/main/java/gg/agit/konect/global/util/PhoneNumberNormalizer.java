package gg.agit.konect.global.util;

public final class PhoneNumberNormalizer {

    private static final int PHONE_NUMBER_MIN_DIGITS = 9;
    private static final int PHONE_NUMBER_MAX_DIGITS = 11;

    private static final int DIGITS_9 = PHONE_NUMBER_MIN_DIGITS;
    private static final int DIGITS_10 = PHONE_NUMBER_MIN_DIGITS + 1;
    private static final int DIGITS_11 = PHONE_NUMBER_MAX_DIGITS;

    private static final int IDX_2 = 2;
    private static final int IDX_3 = 3;
    private static final int IDX_5 = 5;
    private static final int IDX_6 = 6;
    private static final int IDX_7 = 7;

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
     * 전화번호에서 숫자만 추출한 뒤 길이와 패턴에 따라 하이픈(-)을 삽입하여 포맷팅합니다.
     * 구글 시트에서 앞자리 0이 잘린 경우(10자리, 10으로 시작)도 복구합니다.
     *
     * <p>포맷 규칙은 다음과 같습니다.
     * <ul>
     *   <li>구글 시트에서 앞자리 0이 잘린 10자리 번호(10으로 시작): 앞에 0을 붙여 11자리로 복구</li>
     *   <li>11자리: XXX-XXXX-XXXX</li>
     *   <li>서울 지역번호 02, 10자리: 02-XXXX-XXXX</li>
     *   <li>서울 지역번호 02, 9자리: 02-XXX-XXXX</li>
     *   <li>그 외 10자리: XXX-XXX-XXXX</li>
     *   <li>위 조건에 모두 해당하지 않는 경우: 하이픈 없이 숫자만 그대로 반환</li>
     * </ul>
     *
     * <p>예)
     * 1012345678  -> 010-1234-5678  (구글 시트에서 0이 잘린 경우 복구)
     * 01012345678 -> 010-1234-5678
     * 0212345678  -> 02-1234-5678
     * 021234567   -> 02-123-4567
     * 0311234567  -> 031-123-4567
     */
    public static String format(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.trim().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return "";
        }

        // 구글 시트에서 앞자리 0이 잘린 경우 복구: 10자리이고 10으로 시작 (010 -> 10)
        if (digits.length() == DIGITS_10 && digits.startsWith("10")) {
            digits = "0" + digits;
        }

        // 11자리: XXX-XXXX-XXXX
        if (digits.length() == DIGITS_11) {
            return digits.substring(0, IDX_3)
                + "-" + digits.substring(IDX_3, IDX_7)
                + "-" + digits.substring(IDX_7);
        }

        // 서울 지역번호 02: 02-XXXX-XXXX(10자리) or 02-XXX-XXXX(9자리)
        if (digits.startsWith("02")) {
            if (digits.length() == DIGITS_10) {
                return digits.substring(0, IDX_2)
                    + "-" + digits.substring(IDX_2, IDX_6)
                    + "-" + digits.substring(IDX_6);
            }
            if (digits.length() == DIGITS_9) {
                return digits.substring(0, IDX_2)
                    + "-" + digits.substring(IDX_2, IDX_5)
                    + "-" + digits.substring(IDX_5);
            }
        }

        // 기타 10자리: XXX-XXX-XXXX
        if (digits.length() == DIGITS_10) {
            return digits.substring(0, IDX_3)
                + "-" + digits.substring(IDX_3, IDX_6)
                + "-" + digits.substring(IDX_6);
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
