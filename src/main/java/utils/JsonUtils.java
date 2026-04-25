package utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtils {
    private JsonUtils() {}

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value);
        return out.toString();
    }

    public static Object parse(String json) {
        if (json == null) {
            return null;
        }
        return new Parser(json).parse();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        return value instanceof List ? (List<Object>) value : List.of();
    }

    public static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void writeValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
            return;
        }
        if (value instanceof String) {
            writeString(out, (String) value);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
            return;
        }
        if (value instanceof Enum<?>) {
            writeString(out, ((Enum<?>) value).name());
            return;
        }
        if (value instanceof Map<?, ?>) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) out.append(',');
                first = false;
                writeString(out, String.valueOf(entry.getKey()));
                out.append(':');
                writeValue(out, entry.getValue());
            }
            out.append('}');
            return;
        }
        if (value instanceof Iterable<?>) {
            out.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) out.append(',');
                first = false;
                writeValue(out, item);
            }
            out.append(']');
            return;
        }
        if (value.getClass().isArray()) {
            out.append('[');
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                if (i > 0) out.append(',');
                writeValue(out, Array.get(value, i));
            }
            out.append(']');
            return;
        }

        writeString(out, String.valueOf(value));
    }

    private static void writeString(StringBuilder out, String s) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String json;
        private int pos;

        private Parser(String json) {
            this.json = json;
        }

        private Object parse() {
            skipWs();
            Object value = parseValue();
            skipWs();
            return value;
        }

        private Object parseValue() {
            skipWs();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);
            if (c == '"') return parseString();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == 't' && match("true")) return Boolean.TRUE;
            if (c == 'f' && match("false")) return Boolean.FALSE;
            if (c == 'n' && match("null")) return null;
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> out = new LinkedHashMap<>();
            skipWs();
            if (peek('}')) {
                pos++;
                return out;
            }
            while (pos < json.length()) {
                skipWs();
                String key = parseString();
                skipWs();
                expect(':');
                Object value = parseValue();
                out.put(key, value);
                skipWs();
                if (peek(',')) {
                    pos++;
                    continue;
                }
                if (peek('}')) {
                    pos++;
                    break;
                }
                break;
            }
            return out;
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> out = new ArrayList<>();
            skipWs();
            if (peek(']')) {
                pos++;
                return out;
            }
            while (pos < json.length()) {
                out.add(parseValue());
                skipWs();
                if (peek(',')) {
                    pos++;
                    continue;
                }
                if (peek(']')) {
                    pos++;
                    break;
                }
                break;
            }
            return out;
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos++);
                if (c == '"') return out.toString();
                if (c == '\\') {
                    if (pos >= json.length()) break;
                    char esc = json.charAt(pos++);
                    switch (esc) {
                        case '"': out.append('"'); break;
                        case '\\': out.append('\\'); break;
                        case '/': out.append('/'); break;
                        case 'b': out.append('\b'); break;
                        case 'f': out.append('\f'); break;
                        case 'n': out.append('\n'); break;
                        case 'r': out.append('\r'); break;
                        case 't': out.append('\t'); break;
                        case 'u':
                            if (pos + 4 <= json.length()) {
                                String hex = json.substring(pos, pos + 4);
                                out.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                            break;
                        default:
                            out.append(esc);
                    }
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String raw = json.substring(start, pos);
            if (raw.isEmpty()) return 0;
            if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                try {
                    return Double.parseDouble(raw);
                } catch (NumberFormatException ex) {
                    return 0;
                }
            }
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ex) {
                try {
                    return Double.parseDouble(raw);
                } catch (NumberFormatException ex2) {
                    return 0;
                }
            }
        }

        private boolean match(String token) {
            if (json.startsWith(token, pos)) {
                pos += token.length();
                return true;
            }
            return false;
        }

        private void skipWs() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        }

        private void expect(char c) {
            skipWs();
            if (pos >= json.length() || json.charAt(pos) != c) {
                throw new IllegalStateException("Invalid JSON near position " + pos);
            }
            pos++;
        }

        private boolean peek(char c) {
            skipWs();
            return pos < json.length() && json.charAt(pos) == c;
        }
    }
}
