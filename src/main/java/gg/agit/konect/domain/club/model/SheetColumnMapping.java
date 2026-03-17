package gg.agit.konect.domain.club.model;

import java.util.HashMap;
import java.util.Map;

public class SheetColumnMapping {

    public static final String NAME = "name";
    public static final String STUDENT_ID = "studentId";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String POSITION = "position";
    public static final String JOINED_AT = "joinedAt";
    public static final String FEE_PAID = "feePaid";
    public static final String PAID_AT = "paidAt";

    private static final int COL_NAME = 0;
    private static final int COL_STUDENT_ID = 1;
    private static final int COL_EMAIL = 2;
    private static final int COL_PHONE = 3;
    private static final int COL_POSITION = 4;
    private static final int COL_JOINED_AT = 5;
    private static final int COL_FEE_PAID = 6;
    private static final int COL_PAID_AT = 7;
    private static final int DEFAULT_DATA_START_ROW = 2;

    private final Map<String, Integer> fieldToColumn;
    private final int dataStartRow;

    public SheetColumnMapping(Map<String, Integer> fieldToColumn, int dataStartRow) {
        this.fieldToColumn = new HashMap<>(fieldToColumn);
        this.dataStartRow = dataStartRow;
    }

    public SheetColumnMapping(Map<String, Integer> fieldToColumn) {
        this(fieldToColumn, DEFAULT_DATA_START_ROW);
    }

    public static SheetColumnMapping defaultMapping() {
        Map<String, Integer> mapping = new HashMap<>();
        mapping.put(NAME, COL_NAME);
        mapping.put(STUDENT_ID, COL_STUDENT_ID);
        mapping.put(EMAIL, COL_EMAIL);
        mapping.put(PHONE, COL_PHONE);
        mapping.put(POSITION, COL_POSITION);
        mapping.put(JOINED_AT, COL_JOINED_AT);
        mapping.put(FEE_PAID, COL_FEE_PAID);
        mapping.put(PAID_AT, COL_PAID_AT);
        return new SheetColumnMapping(mapping, DEFAULT_DATA_START_ROW);
    }

    public boolean hasColumn(String field) {
        return fieldToColumn.containsKey(field);
    }

    public int getColumnIndex(String field) {
        return fieldToColumn.getOrDefault(field, -1);
    }

    public int getDataStartRow() {
        return dataStartRow;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>(fieldToColumn);
        result.put("dataStartRow", dataStartRow);
        return result;
    }
}
