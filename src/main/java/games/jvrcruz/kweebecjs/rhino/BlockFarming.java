package games.jvrcruz.kweebecjs.rhino;

import java.util.Map;

public final class BlockFarming extends BlockPropertyObject {
    private BlockFarming(Map<String, String> fields) { super(fields); }
    public static Builder builder() { return new Builder(); }
    public static final class Builder extends BlockPropertyObject.Builder<Builder> {
        private Builder() {}
        @Override protected Builder self() { return this; }
        public BlockFarming build() { return new BlockFarming(snapshot()); }
    }
}

