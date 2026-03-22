package gg.agit.konect.domain.club.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public final class SpreadsheetUrlParser {

    private static final Pattern SPREADSHEET_ID_PATTERN =
        Pattern.compile("/spreadsheets/(?:u/\\d+/)?d/([a-zA-Z0-9_-]+)");

    private SpreadsheetUrlParser() {
    }

    public static String extractId(String url) {
        Matcher m = SPREADSHEET_ID_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        throw CustomException.of(ApiResponseCode.INVALID_REQUEST_BODY);
    }
}
