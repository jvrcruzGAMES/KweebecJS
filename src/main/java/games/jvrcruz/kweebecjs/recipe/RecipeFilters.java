package games.jvrcruz.kweebecjs.recipe;

import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

final class RecipeFilters {
    private RecipeFilters() {
    }

    static BenchRequirement parseBenchRequirementSpecArray(NativeArray requirementsArray) {
        BenchType benchType = null;
        String benchId = null;
        List<String> categories = new ArrayList<>();

        for (Object rawSpec : requirementsArray.toArray()) {
            Object unwrapped = unwrap(rawSpec);
            if (!(unwrapped instanceof BenchRequirementSpec spec)) {
                throw new IllegalArgumentException("Bench requirement arrays must contain BenchRequirement values only.");
            }

            switch (spec.kind()) {
                case TYPE -> benchType = (BenchType) spec.value();
                case ID -> benchId = String.valueOf(spec.value());
                case CATEGORY -> categories.add(String.valueOf(spec.value()));
            }
        }

        if (benchType == null) {
            throw new IllegalArgumentException("BenchRequirement.typeRequirement(...) is required.");
        }
        if (benchId == null || benchId.isBlank()) {
            throw new IllegalArgumentException("BenchRequirement.idRequirement(...) is required.");
        }

        return new BenchRequirement(benchType, benchId, categories.toArray(new String[0]), 0);
    }

    static boolean sameBenchRequirement(BenchRequirement left, BenchRequirement right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.type == right.type
                && String.valueOf(left.id).equals(String.valueOf(right.id))
                && sameStringMultiset(toList(left.categories), toList(right.categories));
    }

    static boolean matchesBenchRequirement(BenchRequirement actual, BenchRequirement expected) {
        if (actual == null || expected == null) {
            return false;
        }
        if (actual.type != expected.type) {
            return false;
        }
        if (!String.valueOf(actual.id).equals(String.valueOf(expected.id))) {
            return false;
        }
        List<String> expectedCategories = toList(expected.categories);
        if (expectedCategories.isEmpty()) {
            return true;
        }
        Set<String> actualCategories = new HashSet<>(toList(actual.categories));
        return actualCategories.containsAll(expectedCategories);
    }

    static boolean sameStringMultiset(List<String> left, List<String> right) {
        if (left.size() != right.size()) {
            return false;
        }

        List<String> leftCopy = new ArrayList<>(left);
        List<String> rightCopy = new ArrayList<>(right);
        leftCopy.sort(String::compareTo);
        rightCopy.sort(String::compareTo);
        return leftCopy.equals(rightCopy);
    }

    private static List<String> toList(String[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        return Arrays.asList(values);
    }

    private static Object unwrap(Object value) {
        if (value instanceof Wrapper wrapper) {
            return wrapper.unwrap();
        }
        if (value instanceof NativeJavaObject javaObject) {
            return javaObject.unwrap();
        }
        return value;
    }
}
