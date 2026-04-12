package xyz.jphil.datahelper.json;

import xyz.jphil.datahelper.DataHelper_I;

import java.util.List;
import java.util.Map;

/**
 * Minimal JSON writer that converts Java objects to JSON strings.
 * Uses DataHelper_I property accessors for serialization.
 *
 * <p>This is a simple, focused JSON writer optimized for DataHelper DTOs.
 * It handles the most common JSON types and structures.</p>
 *
 * <p><strong>Supported types:</strong></p>
 * <ul>
 *   <li>Primitives: String, Number (Integer, Long, Double, Float, etc.), Boolean</li>
 *   <li>null values</li>
 *   <li>DataHelper_I objects (recursive)</li>
 *   <li>List (recursive)</li>
 *   <li>Map with String keys (recursive)</li>
 * </ul>
 *
 * <p><strong>Not supported:</strong></p>
 * <ul>
 *   <li>Circular references (will cause StackOverflowError)</li>
 *   <li>Custom serialization logic</li>
 *   <li>Date/Time formatting (serialize as long/string before calling)</li>
 * </ul>
 */
public class MinimalJsonWriter {

    /**
     * Write any Java value to JSON string.
     *
     * @param value the value to serialize
     * @return JSON string representation
     */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    /**
     * Write a DataHelper_I object to JSON string.
     *
     * @param dataHelper the DataHelper object
     * @param deep if true, recursively serialize nested objects
     * @return JSON string representation
     */
    public static String write(DataHelper_I<?> dataHelper, boolean deep) {
        StringBuilder sb = new StringBuilder();
        writeDataHelper(sb, dataHelper, deep);
        return sb.toString();
    }

    // ========== Internal Writing Methods ==========

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Number) {
            writeNumber(sb, (Number) value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof DataHelper_I) {
            writeDataHelper(sb, (DataHelper_I<?>) value, true);
        } else if (value instanceof List) {
            writeList(sb, (List<?>) value);
        } else if (value instanceof Map) {
            writeMap(sb, (Map<?, ?>) value);
        } else {
            // Fallback: toString() and escape
            writeString(sb, value.toString());
        }
    }

    private static void writeDataHelper(StringBuilder sb, DataHelper_I<?> dataHelper, boolean deep) {
        sb.append('{');
        boolean first = true;

        for (String fieldName : dataHelper.fieldNames()) {
            Object value = dataHelper.getPropertyByName(fieldName);

            // Skip null values to reduce JSON size
            if (value == null) continue;

            if (!first) {
                sb.append(',');
            }
            first = false;

            // Write field name
            writeString(sb, fieldName);
            sb.append(':');

            // Write field value
            if (deep && value instanceof DataHelper_I) {
                writeDataHelper(sb, (DataHelper_I<?>) value, true);
            } else if (deep && value instanceof List && dataHelper.isListField(fieldName)) {
                writeList(sb, (List<?>) value);
            } else if (deep && value instanceof Map && dataHelper.isMapField(fieldName)) {
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
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeValue(sb, item);
        }

        sb.append(']');
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;

            // Write key (convert to string)
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');

            // Write value
            writeValue(sb, entry.getValue());
        }

        sb.append('}');
    }

    private static void writeString(StringBuilder sb, String str) {
        sb.append('"');

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20 || c == 0x7F) {
                        // Control characters - encode as unicode
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }

        sb.append('"');
    }

    private static void writeNumber(StringBuilder sb, Number number) {
        // Handle NaN and Infinity (not valid JSON, but handle gracefully)
        if (number instanceof Double) {
            double d = number.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
                return;
            }
        } else if (number instanceof Float) {
            float f = number.floatValue();
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                sb.append("null");
                return;
            }
        }

        // Write number as-is
        sb.append(number.toString());
    }
}
