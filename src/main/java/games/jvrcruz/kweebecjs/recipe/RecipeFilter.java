package games.jvrcruz.kweebecjs.recipe;

import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import games.jvrcruz.kweebecjs.rhino.ScriptItem;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Wrapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecipeFilter {
    private static final Field primaryOutputQuantityField;
    static {
        try {
            primaryOutputQuantityField = CraftingRecipe.class.getDeclaredField("primaryOutputQuantity");
            primaryOutputQuantityField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Could not access CraftingRecipe primaryOutputQuantity.", e);
        }
    }

    private ScriptItem outputItem;
    private List<ScriptItem> inputItems;
    private List<BenchRequirement> benchRequirements;

    public static RecipeFilter create() {
        return new RecipeFilter();
    }

    public RecipeFilter outputs(Object output) {
        Object unwrapped = unwrap(output);
        if (!(unwrapped instanceof ScriptItem scriptItem)) {
            throw new IllegalArgumentException("RecipeFilter.outputs(Item) expects an Item(...).");
        }
        this.outputItem = scriptItem;
        return this;
    }

    public RecipeFilter inputs(Object inputs) {
        if (!(inputs instanceof NativeArray inputArray)) {
            throw new IllegalArgumentException("RecipeFilter.inputs([Item...]) expects an array of Item(...).");
        }

        List<ScriptItem> parsedInputs = new ArrayList<>((int) inputArray.size());
        for (Object value : inputArray.toArray()) {
            Object unwrapped = unwrap(value);
            if (!(unwrapped instanceof ScriptItem scriptItem)) {
                throw new IllegalArgumentException("RecipeFilter.inputs([Item...]) expects only Item(...) values.");
            }
            parsedInputs.add(scriptItem);
        }
        this.inputItems = parsedInputs;
        return this;
    }

    public RecipeFilter benchRequires(Object benchRequirements) {
        if (!(benchRequirements instanceof NativeArray requirementsArray)) {
            throw new IllegalArgumentException("RecipeFilter.benchRequires([BenchRequirement...]) expects an array.");
        }

        BenchRequirement requirement = RecipeFilters.parseBenchRequirementSpecArray(requirementsArray);
        this.benchRequirements = List.of(requirement);
        return this;
    }

    public boolean matches(CraftingRecipe recipe) {
        return matchesOutput(recipe) && matchesInputs(recipe) && matchesBenchRequirements(recipe);
    }

    public List<BenchRequirement> benchRequirements() {
        return benchRequirements == null ? List.of() : List.copyOf(benchRequirements);
    }

    private boolean matchesOutput(CraftingRecipe recipe) {
        if (outputItem == null) {
            return true;
        }

        MaterialQuantity primaryOutput = recipe.getPrimaryOutput();
        if (primaryOutput == null || !outputItem.getID().equals(primaryOutput.getItemId())) {
            return false;
        }
        return !outputItem.hasExplicitAmount() || outputItem.getAmount() == recipePrimaryOutputQuantity(recipe);
    }

    private boolean matchesInputs(CraftingRecipe recipe) {
        if (inputItems == null) {
            return true;
        }

        MaterialQuantity[] recipeInputs = recipe.getInput();
        return sameInputTotals(inputItems, recipeInputs);
    }

    private boolean matchesBenchRequirements(CraftingRecipe recipe) {
        if (benchRequirements == null) {
            return true;
        }

        BenchRequirement[] recipeBenchRequirements = recipe.getBenchRequirement();
        if (recipeBenchRequirements == null || recipeBenchRequirements.length < benchRequirements.size()) {
            return false;
        }

        List<BenchRequirement> recipeRequirementList = Arrays.asList(recipeBenchRequirements);
        for (BenchRequirement expected : benchRequirements) {
            boolean matched = recipeRequirementList.stream().anyMatch(actual -> RecipeFilters.sameBenchRequirement(actual, expected));
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private Object unwrap(Object value) {
        if (value instanceof Wrapper wrapper) {
            return wrapper.unwrap();
        }
        if (value instanceof NativeJavaObject javaObject) {
            return javaObject.unwrap();
        }
        return value;
    }

    private int recipePrimaryOutputQuantity(CraftingRecipe recipe) {
        try {
            return primaryOutputQuantityField.getInt(recipe);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read CraftingRecipe primaryOutputQuantity.", e);
        }
    }

    private boolean sameInputTotals(List<ScriptItem> expectedItems, MaterialQuantity[] actualInputs) {
        if (actualInputs == null) {
            return expectedItems.isEmpty();
        }

        Map<String, Integer> expectedTotals = new HashMap<>();
        for (ScriptItem expectedItem : expectedItems) {
            expectedTotals.merge(expectedItem.getID(), expectedItem.getAmount(), Integer::sum);
        }

        Map<String, Integer> actualTotals = new HashMap<>();
        for (MaterialQuantity actualInput : actualInputs) {
            if (actualInput.getItemId() == null) {
                return false;
            }
            actualTotals.merge(actualInput.getItemId(), actualInput.getQuantity(), Integer::sum);
        }

        return expectedTotals.equals(actualTotals);
    }
}
