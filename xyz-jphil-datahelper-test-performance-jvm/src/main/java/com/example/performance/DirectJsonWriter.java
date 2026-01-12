package com.example.performance;

import xyz.jphil.datahelper.DataHelper_I;
import java.util.List;
import java.util.Map;

/**
 * Direct JSON writer using DataHelper's generated methods.
 * Bypasses Map creation - writes JSON directly from object using:
 * - fieldNames()
 * - getPropertyByName()
 * - getPropertyType()
 * - isListField()
 * - isNestedObjectField()
 * - isMapField()
 */
public class DirectJsonWriter {

    /**
     * Convert DataHelper object directly to JSON string.
     * Uses generated property accessors - NO intermediate Map!
     */
    public static String toJson(DataHelper_I<?> obj) {
        StringBuilder sb = new StringBuilder();
        writeObject(sb, obj);
        return sb.toString();
    }

    private static void writeObject(StringBuilder sb, DataHelper_I<?> obj) {
        sb.append('{');
        boolean first = true;

        for (String fieldName : obj.fieldNames()) {
            Object value = obj.getPropertyByName(fieldName);

            if (value == null) continue; // Skip null fields

            if (!first) sb.append(',');
            first = false;

            // Write field name
            sb.append('"').append(fieldName).append("\":");

            // Write field value based on type
            if (obj.isNestedObjectField(fieldName) && value instanceof DataHelper_I) {
                writeObject(sb, (DataHelper_I<?>) value);
            } else if (obj.isListField(fieldName) && value instanceof List) {
                writeList(sb, (List<?>) value);
            } else if (obj.isMapField(fieldName) && value instanceof Map) {
                writeMap(sb, (Map<?, ?>) value);
            } else {
                writeValue(sb, value);
            }
        }

        sb.append('}');
    }

    private static void writeList(StringBuilder sb, List<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(',');
            first = false;

            if (item instanceof DataHelper_I) {
                writeObject(sb, (DataHelper_I<?>) item);
            } else {
                writeValue(sb, item);
            }
        }
        sb.append(']');
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;

            sb.append('"').append(entry.getKey().toString()).append("\":");

            if (entry.getValue() instanceof DataHelper_I) {
                writeObject(sb, (DataHelper_I<?>) entry.getValue());
            } else {
                writeValue(sb, entry.getValue());
            }
        }
        sb.append('}');
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append('"').append(escape((String) value)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else {
            sb.append('"').append(value.toString()).append('"');
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
