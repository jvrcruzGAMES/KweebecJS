package games.jvrcruz.kweebecjs.rhino;

import java.util.Map;

public final class BlockPlacementSettings extends BlockPropertyObject {
    private BlockPlacementSettings(Map<String, String> fields) { super(fields); }
    public static Builder builder() { return new Builder(); }
    public static final class Builder extends BlockPropertyObject.Builder<Builder> {
        private Builder() {}
        @Override protected Builder self() { return this; }
        public BlockPlacementSettings build() { return new BlockPlacementSettings(snapshot()); }
    }
}

