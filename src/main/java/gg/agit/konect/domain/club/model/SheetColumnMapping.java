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

    private final Map<String, Integer> fieldToColumn;

    public SheetColumnMapping(Map<String, Integer> fieldToColumn) {
        this.fieldToColumn = new HashMap<>(fieldToColumn);
    }

    public static SheetColumnMapping defaultMapping() {
        Map<String, Integer> mapping = new HashMap<>();
        mapping.put(NAME, 0);
        mapping.put(STUDENT_ID, 1);
        mapping.put(EMAIL, 2);
        mapping.put(PHONE, 3);
        mapping.put(POSITION, 4);
        mapping.put(JOINED_AT, 5);
        mapping.put(FEE_PAID, 6);
        mapping.put(PAID_AT, 7);
        return new SheetColumnMapping(mapping);
    }

    public boolean hasColumn(String field) {
        return fieldToColumn.containsKey(field);
    }

    public int getColumnIndex(String field) {
        return fieldToColumn.getOrDefault(field, -1);
    }

    public Map<String, Integer> toMap() {
        return new HashMap<>(fieldToColumn);
    }
}
