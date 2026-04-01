package games.jvrcruz.kweebecjs.rhino;

import java.util.Map;

public final class BlockState extends BlockPropertyObject {
    private BlockState(Map<String, String> fields) { super(fields); }
    public static Builder builder() { return new Builder(); }
    public static final class Builder extends BlockPropertyObject.Builder<Builder> {
        private Builder() {}
        @Override protected Builder self() { return this; }
        public BlockState build() { return new BlockState(snapshot()); }
    }
}

