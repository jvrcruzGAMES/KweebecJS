package games.jvrcruz.kweebecjs.recipe;

import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import games.jvrcruz.kweebecjs.rhino.ScriptItem;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RecipeFilter {
    private String outputItemId;
    private List<String> inputItemIds;
    private List<BenchRequirement> benchRequirements;

    public static RecipeFilter create() {
        return new RecipeFilter();
    }

    public RecipeFilter outputs(Object output) {
        Object unwrapped = unwrap(output);
        if (!(unwrapped instanceof ScriptItem scriptItem)) {
            throw new IllegalArgumentException("RecipeFilter.outputs(Item) expects an Item(...).");
        }
        this.outputItemId = scriptItem.getID();
        return this;
    }

    public RecipeFilter inputs(Object inputs) {
        if (!(inputs instanceof NativeArray inputArray)) {
            throw new IllegalArgumentException("RecipeFilter.inputs([Item...]) expects an array of Item(...).");
        }

        List<String> parsedInputs = new ArrayList<>((int) inputArray.size());
        for (Object value : inputArray.toArray()) {
            Object unwrapped = unwrap(value);
            if (!(unwrapped instanceof ScriptItem scriptItem)) {
                throw new IllegalArgumentException("RecipeFilter.inputs([Item...]) expects only Item(...) values.");
            }
            parsedInputs.add(scriptItem.getID());
        }
        this.inputItemIds = parsedInputs;
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
        if (outputItemId == null) {
            return true;
        }

        MaterialQuantity primaryOutput = recipe.getPrimaryOutput();
        return primaryOutput != null && outputItemId.equals(primaryOutput.getItemId());
    }

    private boolean matchesInputs(CraftingRecipe recipe) {
        if (inputItemIds == null) {
            return true;
        }

        MaterialQuantity[] recipeInputs = recipe.getInput();
        if (recipeInputs == null || recipeInputs.length != inputItemIds.size()) {
            return false;
        }

        List<String> recipeItemIds = new ArrayList<>(recipeInputs.length);
        for (MaterialQuantity recipeInput : recipeInputs) {
            recipeItemIds.add(recipeInput.getItemId());
        }
        return RecipeFilters.sameStringMultiset(recipeItemIds, inputItemIds);
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
}
