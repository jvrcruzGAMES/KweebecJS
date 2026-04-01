package games.jvrcruz.kweebecjs.rhino;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class BlockArray implements BlockJsonValue {
    private final List<String> values;

    private BlockArray(List<String> values) {
        this.values = List.copyOf(values);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toJson() {
        return "[" + values.stream().collect(Collectors.joining(", ")) + "]";
    }

    public static final class Builder {
        private final List<String> values = new ArrayList<>();

        private Builder() {
        }

        public Builder string(String value) {
            if (value == null) {
                throw new IllegalArgumentException("value must not be null.");
            }
            values.add("\"" + escapeJson(value) + "\"");
            return this;
        }

        public Builder number(double value) {
            String formatted = value == (long) value ? Long.toString((long) value) : Double.toString(value);
            values.add(formatted);
            return this;
        }

        public Builder integer(int value) {
            values.add(Integer.toString(value));
            return this;
        }

        public Builder bool(boolean value) {
            values.add(Boolean.toString(value));
            return this;
        }

        public Builder nullValue() {
            values.add("null");
            return this;
        }

        public Builder object(BlockJsonValue value) {
            if (value == null) {
                throw new IllegalArgumentException("value must not be null.");
            }
            values.add(value.toJson());
            return this;
        }

        public Builder array(BlockArray value) {
            if (value == null) {
                throw new IllegalArgumentException("value must not be null.");
            }
            values.add(value.toJson());
            return this;
        }

        public BlockArray build() {
            return new BlockArray(values);
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
