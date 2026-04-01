package games.jvrcruz.kweebecjs.rhino;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

abstract class BlockPropertyObject implements BlockJsonValue {
    private final Map<String, String> fields;

    protected BlockPropertyObject(Map<String, String> fields) {
        this.fields = Map.copyOf(fields);
    }

    @Override
    public final String toJson() {
        return "{ " + fields.entrySet().stream()
                .map(entry -> "\"" + escapeJson(entry.getKey()) + "\": " + entry.getValue())
                .collect(Collectors.joining(", ")) + " }";
    }

    protected static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    abstract static class Builder<B extends Builder<B>> {
        private final Map<String, String> fields = new LinkedHashMap<>();

        protected abstract B self();

        public B string(String key, String value) {
            requireKey(key);
            if (value == null) {
                throw new IllegalArgumentException("value must not be null.");
            }
            fields.put(key, "\"" + escapeJson(value) + "\"");
            return self();
        }

        public B number(String key, double value) {
            requireKey(key);
            String formatted = value == (long) value ? Long.toString((long) value) : Double.toString(value);
            fields.put(key, formatted);
            return self();
        }

        public B integer(String key, int value) {
            requireKey(key);
            fields.put(key, Integer.toString(value));
            return self();
        }

        public B bool(String key, boolean value) {
            requireKey(key);
            fields.put(key, Boolean.toString(value));
            return self();
        }

        public B nullValue(String key) {
            requireKey(key);
            fields.put(key, "null");
            return self();
        }

        public B object(String key, BlockJsonValue value) {
            requireKey(key);
            if (value == null) {
                throw new IllegalArgumentException("value must not be null.");
            }
            fields.put(key, value.toJson());
            return self();
        }

        public B array(String key, BlockArray value) {
            requireKey(key);
            if (value == null) {
                throw new IllegalArgumentException("value must not be null.");
            }
            fields.put(key, value.toJson());
            return self();
        }

        protected final Map<String, String> snapshot() {
            return new LinkedHashMap<>(fields);
        }

        private static void requireKey(String key) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key must not be blank.");
            }
        }
    }
}
