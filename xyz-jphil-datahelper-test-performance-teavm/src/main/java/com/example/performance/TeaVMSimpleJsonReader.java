package com.example.performance;

import java.util.*;

/**
 * Simple JSON parser for TeaVM - minimal implementation.
 * Returns Map that can be used with DataHelper's fromMap().
 * TeaVM-compatible (no reflection, works in JavaScript).
 */
public class TeaVMSimpleJsonReader {

    private String json;
    private int pos;

    public static Map<String, Object> fromJson(String json) {
        TeaVMSimpleJsonReader reader = new TeaVMSimpleJsonReader(json);
        return reader.readObject();
    }

    /**
     * Convenience method for SimpleDTOTeaVM
     */
    public static SimpleDTOTeaVM fromJsonToSimpleDTO(String json) {
        Map<String, Object> map = fromJson(json);
        return new SimpleDTOTeaVM().fromMap(map);
    }

    private TeaVMSimpleJsonReader(String json) {
        this.json = json;
        this.pos = 0;
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        consume('{');
        skipWhitespace();

        if (peek() != '}') {
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                consume(':');
                skipWhitespace();
                Object value = readValue();
                map.put(key, value);
                skipWhitespace();

                char next = peek();
                if (next == '}') break;
                if (next == ',') {
                    consume(',');
                } else {
                    break;
                }
            }
        }

        consume('}');
        return map;
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        consume('[');
        skipWhitespace();

        if (peek() != ']') {
            while (true) {
                skipWhitespace();
                Object value = readValue();
                list.add(value);
                skipWhitespace();

                char next = peek();
                if (next == ']') break;
                if (next == ',') {
                    consume(',');
                } else {
                    break;
                }
            }
        }

        consume(']');
        return list;
    }

    private Object readValue() {
        skipWhitespace();
        char c = peek();

        if (c == '"') {
            return readString();
        } else if (c == '{') {
            return readObject();
        } else if (c == '[') {
            return readArray();
        } else if (c == 't' || c == 'f') {
            return readBoolean();
        } else if (c == 'n') {
            return readNull();
        } else if (c == '-' || Character.isDigit(c)) {
            return readNumber();
        }

        throw new RuntimeException("Unexpected character: " + c);
    }

    private String readString() {
        consume('"');
        StringBuilder sb = new StringBuilder();

        while (peek() != '"') {
            char c = consume();
            if (c == '\\') {
                char escaped = consume();
                switch (escaped) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }

        consume('"');
        return sb.toString();
    }

    private Number readNumber() {
        int start = pos;
        if (peek() == '-') consume();

        while (Character.isDigit(peek())) consume();

        boolean isDouble = false;
        if (peek() == '.') {
            isDouble = true;
            consume();
            while (Character.isDigit(peek())) consume();
        }

        if (peek() == 'e' || peek() == 'E') {
            isDouble = true;
            consume();
            if (peek() == '+' || peek() == '-') consume();
            while (Character.isDigit(peek())) consume();
        }

        String numStr = json.substring(start, pos);
        return isDouble ? Double.parseDouble(numStr) : Integer.parseInt(numStr);
    }

    private Boolean readBoolean() {
        if (peek() == 't') {
            consume('t');
            consume('r');
            consume('u');
            consume('e');
            return true;
        } else {
            consume('f');
            consume('a');
            consume('l');
            consume('s');
            consume('e');
            return false;
        }
    }

    private Object readNull() {
        consume('n');
        consume('u');
        consume('l');
        consume('l');
        return null;
    }

    private void skipWhitespace() {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= json.length()) return '\0';
        return json.charAt(pos);
    }

    private char consume() {
        return json.charAt(pos++);
    }

    private void consume(char expected) {
        char actual = consume();
        if (actual != expected) {
            throw new RuntimeException("Expected '" + expected + "' but got '" + actual + "'");
        }
    }
}
