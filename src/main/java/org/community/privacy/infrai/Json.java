package org.community.privacy.infrai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private final String source;
    private int offset;

    private Json(String source) {
        this.source = source;
    }

    static Object parse(String source) {
        Json parser = new Json(source);
        Object value = parser.value();
        parser.space();
        if (parser.offset != source.length()) throw parser.error("trailing content");
        return value;
    }

    private Object value() {
        space();
        if (offset >= source.length()) throw error("missing value");
        return switch (source.charAt(offset)) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> result = new LinkedHashMap<>();
        offset++;
        space();
        if (take('}')) return result;
        do {
            space();
            String key = string();
            space();
            expect(':');
            result.put(key, value());
            space();
        } while (take(','));
        expect('}');
        return result;
    }

    private List<Object> array() {
        List<Object> result = new ArrayList<>();
        offset++;
        space();
        if (take(']')) return result;
        do {
            result.add(value());
            space();
        } while (take(','));
        expect(']');
        return result;
    }

    private String string() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (offset < source.length()) {
            char c = source.charAt(offset++);
            if (c == '"') return result.toString();
            if (c != '\\') {
                result.append(c);
                continue;
            }
            if (offset >= source.length()) throw error("unfinished escape");
            char escaped = source.charAt(offset++);
            switch (escaped) {
                case '"', '\\', '/' -> result.append(escaped);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (offset + 4 > source.length()) throw error("unfinished unicode escape");
                    result.append((char) Integer.parseInt(source.substring(offset, offset + 4), 16));
                    offset += 4;
                }
                default -> throw error("invalid escape");
            }
        }
        throw error("unfinished string");
    }

    private Object number() {
        int start = offset;
        while (offset < source.length() && "-+0123456789.eE".indexOf(source.charAt(offset)) >= 0) offset++;
        if (start == offset) throw error("invalid value");
        String token = source.substring(start, offset);
        return token.contains(".") || token.contains("e") || token.contains("E")
                ? Double.parseDouble(token) : Long.parseLong(token);
    }

    private Object literal(String token, Object value) {
        if (!source.startsWith(token, offset)) throw error("invalid literal");
        offset += token.length();
        return value;
    }

    private void space() {
        while (offset < source.length() && Character.isWhitespace(source.charAt(offset))) offset++;
    }

    private boolean take(char expected) {
        if (offset < source.length() && source.charAt(offset) == expected) {
            offset++;
            return true;
        }
        return false;
    }

    private void expect(char expected) {
        if (!take(expected)) throw error("expected " + expected);
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("Invalid JSON at " + offset + ": " + message);
    }
}
