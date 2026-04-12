package xyz.jphil.datahelper.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser that converts JSON strings to Java objects.
 * Used by DataHelper_Json_Trait for deserialization.
 *
 * <p>This is a simple, focused JSON parser that handles the most common JSON structures.
 * It parses JSON into standard Java collections (Map, List) and primitives.</p>
 *
 * <p><strong>Supported JSON types:</strong></p>
 * <ul>
 *   <li>Objects → Map&lt;String, Object&gt;</li>
 *   <li>Arrays → List&lt;Object&gt;</li>
 *   <li>Strings</li>
 *   <li>Numbers (parsed as Double or Long)</li>
 *   <li>Booleans</li>
 *   <li>null</li>
 * </ul>
 *
 * <p><strong>Not supported:</strong></p>
 * <ul>
 *   <li>JSON comments (not part of JSON spec)</li>
 *   <li>Trailing commas</li>
 *   <li>Single-quoted strings</li>
 *   <li>Unquoted keys</li>
 * </ul>
 */
public class MinimalJsonParser {

    private final String json;
    private int pos = 0;

    private MinimalJsonParser(String json) {
        this.json = json;
    }

    /**
     * Parse JSON string to Java object.
     *
     * @param json the JSON string
     * @return parsed object (Map, List, String, Number, Boolean, or null)
     * @throws JsonParseException if JSON is malformed
     */
    public static Object parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new JsonParseException("JSON string is null or empty");
        }

        MinimalJsonParser parser = new MinimalJsonParser(json.trim());
        Object result = parser.parseValue();

        parser.skipWhitespace();
        if (parser.pos < parser.json.length()) {
            throw new JsonParseException("Unexpected characters after JSON value at position " + parser.pos);
        }

        return result;
    }

    /**
     * Parse JSON object string to Map.
     *
     * @param json the JSON object string (must start with '{')
     * @return parsed Map
     * @throws JsonParseException if JSON is not a valid object
     */
    public static Map<String, Object> parseObject(String json) {
        Object result = parse(json);
        if (!(result instanceof Map)) {
            throw new JsonParseException("JSON does not represent an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        return map;
    }

    /**
     * Parse JSON array string to List.
     *
     * @param json the JSON array string (must start with '[')
     * @return parsed List
     * @throws JsonParseException if JSON is not a valid array
     */
    public static List<Object> parseArray(String json) {
        Object result = parse(json);
        if (!(result instanceof List)) {
            throw new JsonParseException("JSON does not represent an array");
        }
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) result;
        return list;
    }

    // ========== Internal Parsing Methods ==========

    private Object parseValue() {
        skipWhitespace();

        if (pos >= json.length()) {
            throw new JsonParseException("Unexpected end of JSON at position " + pos);
        }

        char c = json.charAt(pos);

        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 't':
            case 'f':
                return parseBoolean();
            case 'n':
                return parseNull();
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return parseNumber();
            default:
                throw new JsonParseException("Unexpected character '" + c + "' at position " + pos);
        }
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();

        expect('{');
        skipWhitespace();

        // Empty object
        if (peek() == '}') {
            pos++;
            return map;
        }

        while (true) {
            skipWhitespace();

            // Parse key (must be string)
            if (peek() != '"') {
                throw new JsonParseException("Expected string key at position " + pos);
            }
            String key = parseString();

            skipWhitespace();
            expect(':');
            skipWhitespace();

            // Parse value
            Object value = parseValue();
            map.put(key, value);

            skipWhitespace();
            char next = peek();

            if (next == '}') {
                pos++;
                break;
            } else if (next == ',') {
                pos++;
                // Continue to next key-value pair
            } else {
                throw new JsonParseException("Expected ',' or '}' at position " + pos + ", found '" + next + "'");
            }
        }

        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();

        expect('[');
        skipWhitespace();

        // Empty array
        if (peek() == ']') {
            pos++;
            return list;
        }

        while (true) {
            skipWhitespace();

            // Parse element
            Object value = parseValue();
            list.add(value);

            skipWhitespace();
            char next = peek();

            if (next == ']') {
                pos++;
                break;
            } else if (next == ',') {
                pos++;
                // Continue to next element
            } else {
                throw new JsonParseException("Expected ',' or ']' at position " + pos + ", found '" + next + "'");
            }
        }

        return list;
    }

    private String parseString() {
        expect('"');

        StringBuilder sb = new StringBuilder();

        while (pos < json.length()) {
            char c = json.charAt(pos++);

            if (c == '"') {
                // End of string
                return sb.toString();
            } else if (c == '\\') {
                // Escape sequence
                if (pos >= json.length()) {
                    throw new JsonParseException("Unexpected end of string at position " + pos);
                }

                char escaped = json.charAt(pos++);
                switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        // Unicode escape: backslash-u-XXXX
                        if (pos + 4 > json.length()) {
                            throw new JsonParseException("Invalid unicode escape at position " + pos);
                        }
                        String hex = json.substring(pos, pos + 4);
                        try {
                            int codePoint = Integer.parseInt(hex, 16);
                            sb.append((char) codePoint);
                            pos += 4;
                        } catch (NumberFormatException e) {
                            throw new JsonParseException("Invalid unicode escape sequence " + hex + " at position " + pos);
                        }
                        break;
                    default:
                        throw new JsonParseException("Invalid escape sequence \\" + escaped + " at position " + pos);
                }
            } else {
                sb.append(c);
            }
        }

        throw new JsonParseException("Unterminated string at position " + pos);
    }

    private Number parseNumber() {
        int start = pos;

        // Optional minus
        if (peek() == '-') {
            pos++;
        }

        // Integer part
        if (!isDigit(peek())) {
            throw new JsonParseException("Expected digit at position " + pos);
        }

        if (peek() == '0') {
            pos++;
        } else {
            while (pos < json.length() && isDigit(json.charAt(pos))) {
                pos++;
            }
        }

        // Check for decimal or exponent (makes it a double)
        boolean isDouble = false;

        // Decimal part
        if (pos < json.length() && peek() == '.') {
            isDouble = true;
            pos++;

            if (!isDigit(peek())) {
                throw new JsonParseException("Expected digit after '.' at position " + pos);
            }

            while (pos < json.length() && isDigit(json.charAt(pos))) {
                pos++;
            }
        }

        // Exponent part
        if (pos < json.length() && (peek() == 'e' || peek() == 'E')) {
            isDouble = true;
            pos++;

            if (pos < json.length() && (peek() == '+' || peek() == '-')) {
                pos++;
            }

            if (!isDigit(peek())) {
                throw new JsonParseException("Expected digit in exponent at position " + pos);
            }

            while (pos < json.length() && isDigit(json.charAt(pos))) {
                pos++;
            }
        }

        String numberStr = json.substring(start, pos);

        try {
            if (isDouble) {
                return Double.parseDouble(numberStr);
            } else {
                // Try Long first (handles large integers)
                return Long.parseLong(numberStr);
            }
        } catch (NumberFormatException e) {
            throw new JsonParseException("Invalid number '" + numberStr + "' at position " + start);
        }
    }

    private Boolean parseBoolean() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        } else if (json.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        } else {
            throw new JsonParseException("Expected 'true' or 'false' at position " + pos);
        }
    }

    private Object parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return null;
        } else {
            throw new JsonParseException("Expected 'null' at position " + pos);
        }
    }

    // ========== Helper Methods ==========

    private void skipWhitespace() {
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private char peek() {
        if (pos >= json.length()) {
            throw new JsonParseException("Unexpected end of JSON at position " + pos);
        }
        return json.charAt(pos);
    }

    private void expect(char expected) {
        if (pos >= json.length()) {
            throw new JsonParseException("Expected '" + expected + "' but reached end of JSON");
        }

        char actual = json.charAt(pos);
        if (actual != expected) {
            throw new JsonParseException("Expected '" + expected + "' at position " + pos + ", found '" + actual + "'");
        }

        pos++;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // ========== Exception ==========

    /**
     * Exception thrown when JSON parsing fails.
     */
    public static class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }

        public JsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
