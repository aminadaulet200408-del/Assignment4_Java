import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.space();
        if (parser.index != source.length()) throw parser.error("Unexpected trailing content");
        return value;
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            quote((String) value, out);
        } else if (value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else if (value instanceof Number) {
            Number number = (Number) value;
            double d = number.doubleValue();
            if (!Double.isFinite(d)) throw new IllegalArgumentException("Non-finite JSON number");
            out.append(number);
        } else if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Object entryObject : ((Map<?, ?>) value).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObject;
                if (!(entry.getKey() instanceof String)) throw new IllegalArgumentException("JSON object keys must be strings");
                if (!first) out.append(',');
                first = false;
                quote((String) entry.getKey(), out);
                out.append(':');
                write(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof Iterable) {
            out.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) out.append(',');
                first = false;
                write(item, out);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
        }
    }

    private static void quote(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else out.append(c);
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String source;
        private int index;

        Parser(String source) { this.source = source; }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }

        void space() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++;
        }

        Object value() {
            space();
            if (index >= source.length()) throw error("Expected a value");
            char c = source.charAt(index);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (c == 't') return literal("true", Boolean.TRUE);
            if (c == 'f') return literal("false", Boolean.FALSE);
            if (c == 'n') return literal("null", null);
            if (c == '-' || (c >= '0' && c <= '9')) return number();
            throw error("Unexpected character");
        }

        private Object literal(String word, Object result) {
            if (!source.startsWith(word, index)) throw error("Invalid literal");
            index += word.length();
            return result;
        }

        private Map<String, Object> object() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (take('}')) return result;
            while (true) {
                space();
                if (index >= source.length() || source.charAt(index) != '"') throw error("Expected object key");
                String key = string();
                space();
                if (!take(':')) throw error("Expected ':'");
                result.put(key, value());
                space();
                if (take('}')) return result;
                if (!take(',')) throw error("Expected ',' or '}'");
            }
        }

        private List<Object> array() {
            index++;
            List<Object> result = new ArrayList<>();
            space();
            if (take(']')) return result;
            while (true) {
                result.add(value());
                space();
                if (take(']')) return result;
                if (!take(',')) throw error("Expected ',' or ']'");
            }
        }

        private String string() {
            index++;
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == '"') return result.toString();
                if (c != '\\') {
                    if (c < 0x20) throw error("Unescaped control character");
                    result.append(c);
                    continue;
                }
                if (index >= source.length()) throw error("Incomplete escape");
                char escape = source.charAt(index++);
                switch (escape) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u':
                        if (index + 4 > source.length()) throw error("Incomplete unicode escape");
                        try { result.append((char) Integer.parseInt(source.substring(index, index + 4), 16)); }
                        catch (NumberFormatException ex) { throw error("Invalid unicode escape"); }
                        index += 4;
                        break;
                    default: throw error("Invalid escape");
                }
            }
            throw error("Unterminated string");
        }

        private Long number() {
            int start = index;
            if (take('-') && index >= source.length()) throw error("Invalid number");
            if (take('0')) {
                // A leading zero is the complete integer part.
            } else {
                if (index >= source.length() || !digit(source.charAt(index))) throw error("Invalid number");
                while (index < source.length() && digit(source.charAt(index))) index++;
            }
            if (index < source.length() && (source.charAt(index) == '.' || source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                throw error("Only integer JSON numbers are supported by this assignment");
            }
            try { return Long.parseLong(source.substring(start, index)); }
            catch (NumberFormatException ex) { throw error("Integer is outside signed 64-bit range"); }
        }

        private boolean take(char expected) {
            if (index < source.length() && source.charAt(index) == expected) { index++; return true; }
            return false;
        }

        private boolean digit(char c) { return c >= '0' && c <= '9'; }
    }
}
