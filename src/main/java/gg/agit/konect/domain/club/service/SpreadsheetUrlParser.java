package gg.agit.konect.domain.club.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpreadsheetUrlParser {

    private static final Pattern SPREADSHEET_ID_PATTERN =
        Pattern.compile("/spreadsheets/d/([a-zA-Z0-9_-]+)");

    private SpreadsheetUrlParser() {
    }

    public static String extractId(String urlOrId) {
        Matcher m = SPREADSHEET_ID_PATTERN.matcher(urlOrId);
        if (m.find()) {
            return m.group(1);
        }
        return urlOrId;
    }
}
