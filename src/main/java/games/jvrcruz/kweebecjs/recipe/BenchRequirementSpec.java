package games.jvrcruz.kweebecjs.recipe;

import com.hypixel.hytale.protocol.BenchType;

public class BenchRequirementSpec {
    public enum Kind {
        TYPE,
        ID,
        CATEGORY
    }

    private final Kind kind;
    private final Object value;

    private BenchRequirementSpec(Kind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    public static BenchRequirementSpec typeRequirement(Object benchType) {
        if (benchType instanceof BenchType type) {
            return new BenchRequirementSpec(Kind.TYPE, type);
        }
        return new BenchRequirementSpec(Kind.TYPE, BenchType.valueOf(String.valueOf(benchType)));
    }

    public static BenchRequirementSpec idRequirement(String benchId) {
        return new BenchRequirementSpec(Kind.ID, benchId);
    }

    public static BenchRequirementSpec category(String category) {
        return new BenchRequirementSpec(Kind.CATEGORY, category);
    }

    public static BenchRequirementSpec categoryRequirement(String category) {
        return category(category);
    }

    public Kind kind() {
        return kind;
    }

    public Object value() {
        return value;
    }
}
